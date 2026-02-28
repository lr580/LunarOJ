package com.lunaroj.stress.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class AuthFullChainStressIT {

    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final String DEFAULT_PASSWORD = "StressPass123";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${stress.auth.register-users:500}")
    private int registerUsers;
    @Value("${stress.auth.login-users:500}")
    private int loginUsers;
    @Value("${stress.auth.login-max-seconds:60}")
    private long loginMaxSeconds;
    @Value("${stress.auth.request-timeout-seconds:20}")
    private int requestTimeoutSeconds;
    @Value("${stress.auth.ready-timeout-seconds:30}")
    private int readyTimeoutSeconds;
    @Value("${stress.auth.stage-timeout-seconds:180}")
    private int stageTimeoutSeconds;
    @Value("${stress.auth.captcha-ttl-seconds:300}")
    private long captchaTtlSeconds;

    @Test
    void burstRegisterShouldCreateConfiguredUsersThroughFullChain() throws Exception {
        StressConfig config = loadStressConfigFromYaml();
        String usernamePrefix = buildUsernamePrefix("reg");
        List<UserCredential> users = buildUsers(usernamePrefix, config.registerUsers(), DEFAULT_PASSWORD);

        try {
            assertThat(fetchRegisterEnabled())
                    .as("system_config.register_enabled 必须为 true 才能进行注册压测")
                    .isTrue();

            BurstMetrics metrics = runBurstRegister(users, config);
            assertBurstSucceeded("register", users.size(), metrics);
            Long persistedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM `user` WHERE deleted_at IS NULL AND username LIKE ?",
                    Long.class,
                    usernamePrefix + "%"
            );
            assertThat(persistedCount)
                    .as("注册后数据库落库数量不匹配")
                    .isEqualTo((long) users.size());
        } finally {
            hardDeleteUsersByPrefix(usernamePrefix);
        }
    }

    @Test
    void burstLoginShouldCompleteWithinOneMinuteThroughFullChain() throws Exception {
        StressConfig config = loadStressConfigFromYaml();
        String usernamePrefix = buildUsernamePrefix("login");
        List<UserCredential> users = buildUsers(usernamePrefix, config.loginUsers(), DEFAULT_PASSWORD);

        try {
            assertThat(fetchRegisterEnabled())
                    .as("system_config.register_enabled 必须为 true 才能准备登录压测用户")
                    .isTrue();

            BurstMetrics registerMetrics = runBurstRegister(users, config);
            assertBurstSucceeded("prepare-register-for-login", users.size(), registerMetrics);

            BurstMetrics loginMetrics = runBurstLogin(users, config);
            assertBurstSucceeded("login", users.size(), loginMetrics);
            assertThat(loginMetrics.elapsed())
                    .as("登录阶段耗时必须 <= %s 秒，实际=%s ms", config.loginMaxSeconds(), loginMetrics.elapsed().toMillis())
                    .isLessThanOrEqualTo(Duration.ofSeconds(config.loginMaxSeconds()));
        } finally {
            hardDeleteUsersByPrefix(usernamePrefix);
        }
    }

    private BurstMetrics runBurstRegister(List<UserCredential> users, StressConfig config) throws InterruptedException {
        return runBurst(
                users,
                config,
                credential -> {
                    String captchaId = randomCaptchaId();
                    String captchaCode = randomCaptchaCode();
                    seedCaptcha(captchaId, captchaCode, config.captchaTtlSeconds());

                    RegisterPayload payload = new RegisterPayload(
                            credential.username(),
                            credential.password(),
                            credential.username(),
                            null,
                            captchaId,
                            captchaCode
                    );
                    JsonNode root = postJson("/api/auth/register", payload, config.requestTimeoutSeconds());
                    assertApiSuccess(root);
                }
        );
    }

    private BurstMetrics runBurstLogin(List<UserCredential> users, StressConfig config) throws InterruptedException {
        return runBurst(
                users,
                config,
                credential -> {
                    String captchaId = randomCaptchaId();
                    String captchaCode = randomCaptchaCode();
                    seedCaptcha(captchaId, captchaCode, config.captchaTtlSeconds());

                    LoginPayload payload = new LoginPayload(
                            credential.username(),
                            credential.password(),
                            captchaId,
                            captchaCode
                    );
                    JsonNode root = postJson("/api/auth/login", payload, config.requestTimeoutSeconds());
                    assertApiSuccess(root);

                    JsonNode data = root.path("data");
                    String accessToken = data.path("accessToken").asText("");
                    String refreshToken = data.path("refreshToken").asText("");
                    if (accessToken.isBlank() || refreshToken.isBlank()) {
                        throw new IllegalStateException("登录响应缺少 accessToken/refreshToken");
                    }
                }
        );
    }

    private BurstMetrics runBurst(
            List<UserCredential> users,
            StressConfig config,
            BurstTask task
    ) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(users.size());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(users.size());
        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (UserCredential user : users) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        task.execute(user);
                        successCount.incrementAndGet();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        failures.add("username=" + user.username() + ", error=interrupted");
                    } catch (Exception ex) {
                        failures.add("username=" + user.username() + ", error=" + normalizeMessage(ex));
                    } finally {
                        done.countDown();
                    }
                });
            }

            boolean allReady = ready.await(config.readyTimeoutSeconds(), TimeUnit.SECONDS);
            if (!allReady) {
                failures.add("未在规定时间内完成并发任务准备");
            }

            long startedAtNanos = System.nanoTime();
            start.countDown();

            boolean finished = done.await(config.stageTimeoutSeconds(), TimeUnit.SECONDS);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAtNanos);
            if (!finished) {
                failures.add("阶段在 " + config.stageTimeoutSeconds() + " 秒内未完成");
            }
            return new BurstMetrics(elapsed, successCount.get(), new ArrayList<>(failures));
        }
    }

    private JsonNode postJson(String path, Object payload, int timeoutSeconds) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl(path)))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return executeRequest(request);
    }

    private boolean fetchRegisterEnabled() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl("/api/auth/register-enabled")))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        JsonNode root = executeRequest(request);
        assertApiSuccess(root);
        return root.path("data").asBoolean(false);
    }

    private JsonNode executeRequest(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP status=" + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private void assertApiSuccess(JsonNode root) {
        int code = root.path("code").asInt(Integer.MIN_VALUE);
        if (code != 0) {
            throw new IllegalStateException("code=" + code + ", message=" + root.path("message").asText(""));
        }
    }

    private void seedCaptcha(String captchaId, String captchaCode, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + captchaId,
                captchaCode.toLowerCase(Locale.ROOT),
                Duration.ofSeconds(ttlSeconds)
        );
    }

    private void hardDeleteUsersByPrefix(String usernamePrefix) {
        jdbcTemplate.update("DELETE FROM `user` WHERE username LIKE ?", usernamePrefix + "%");
    }

    private List<UserCredential> buildUsers(String usernamePrefix, int size, String password) {
        List<UserCredential> users = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            users.add(new UserCredential(usernamePrefix + i, password));
        }
        return users;
    }

    private String buildUsernamePrefix(String scene) {
        String runId = Long.toString(System.currentTimeMillis(), 36) + UUID.randomUUID().toString().substring(0, 6);
        return "stress_" + scene + "_" + runId + "_";
    }

    private String randomCaptchaId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String randomCaptchaCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 4).toLowerCase(Locale.ROOT);
    }

    private void assertBurstSucceeded(String stage, int expectedUsers, BurstMetrics metrics) {
        assertThat(metrics.successCount())
                .as("%s 成功数不符合预期", stage)
                .isEqualTo(expectedUsers);
        assertThat(metrics.failures())
                .as("%s 存在失败请求:\n%s", stage, summarizeFailures(metrics.failures()))
                .isEmpty();
    }

    private String summarizeFailures(List<String> failures) {
        if (failures.isEmpty()) {
            return "<none>";
        }
        int total = failures.size();
        String head = failures.stream().limit(20).collect(Collectors.joining(System.lineSeparator()));
        if (total <= 20) {
            return head;
        }
        return head + System.lineSeparator() + "... total failures: " + total;
    }

    private String normalizeMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message.replace(System.lineSeparator(), " ");
    }

    private StressConfig loadStressConfigFromYaml() {
        return new StressConfig(
                positiveIntValue("stress.auth.register-users", registerUsers),
                positiveIntValue("stress.auth.login-users", loginUsers),
                positiveLongValue("stress.auth.login-max-seconds", loginMaxSeconds),
                positiveIntValue("stress.auth.request-timeout-seconds", requestTimeoutSeconds),
                positiveIntValue("stress.auth.ready-timeout-seconds", readyTimeoutSeconds),
                positiveIntValue("stress.auth.stage-timeout-seconds", stageTimeoutSeconds),
                positiveLongValue("stress.auth.captcha-ttl-seconds", captchaTtlSeconds)
        );
    }

    @FunctionalInterface
    private interface BurstTask {
        void execute(UserCredential credential) throws Exception;
    }

    private record UserCredential(String username, String password) {
    }

    private record RegisterPayload(
            String username,
            String password,
            String nickname,
            String email,
            String captchaId,
            String captchaCode
    ) {
    }

    private record LoginPayload(
            String username,
            String password,
            String captchaId,
            String captchaCode
    ) {
    }

    private record BurstMetrics(
            Duration elapsed,
            int successCount,
            List<String> failures
    ) {
    }

    private record StressConfig(
            int registerUsers,
            int loginUsers,
            long loginMaxSeconds,
            int requestTimeoutSeconds,
            int readyTimeoutSeconds,
            int stageTimeoutSeconds,
            long captchaTtlSeconds
    ) {
    }

    private static int positiveIntValue(String key, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("config " + key + " must > 0");
        }
        return value;
    }

    private static long positiveLongValue(String key, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("config " + key + " must > 0");
        }
        return value;
    }

    private String apiUrl(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
