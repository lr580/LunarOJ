package com.lunaroj.migration.cli;

import com.lunaroj.migration.model.ExecutionConfig;
import com.lunaroj.migration.model.MigrationProfile;
import com.lunaroj.migration.model.SourceDatabaseConfig;
import com.lunaroj.migration.module.user.EmailConflictPolicy;
import com.lunaroj.migration.module.user.UserMigrationReport;
import com.lunaroj.migration.module.user.UserMigrationService;
import com.lunaroj.migration.module.user.UserModuleConfig;
import com.lunaroj.migration.module.user.UserPasswordMode;
import com.lunaroj.migration.module.user.UsernameConflictPolicy;
import com.lunaroj.migration.report.MigrationReportWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MigrationCliService {

    private static final DateTimeFormatter PROFILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ConsoleIO io;
    private final MigrationProfileStore profileStore;
    private final UserMigrationService userMigrationService;
    private final MigrationReportWriter reportWriter;

    public void launch() {
        io.println("");
        io.println("[LunarOJ Migration CLI]");
        while (true) {
            io.println("");
            io.println("1) New migration task (wizard)");
            io.println("2) Run from saved profile");
            io.println("3) Exit");
            int choice = io.promptIntWithDefault("Select", 1, 1);

            switch (choice) {
                case 1 -> runWizard();
                case 2 -> runFromSavedProfile();
                case 3 -> {
                    io.println("Exit.");
                    return;
                }
                default -> io.println("Unsupported option.");
            }
        }
    }

    private void runWizard() {
        MigrationProfile profile = new MigrationProfile();
        SourceDatabaseConfig source = profile.getSource();
        ExecutionConfig execution = profile.getExecution();
        UserModuleConfig user = profile.getModules().getUser();

        io.println("");
        io.println("[Source database]");
        source.setHost(io.promptWithDefault("Host", "127.0.0.1"));
        source.setPort(io.promptIntWithDefault("Port", 3306, 1));
        source.setUser(io.prompt("User"));
        source.setPassword(io.promptPassword("Password"));
        source.setDb(io.promptWithDefault("Database", "scnuoj"));
        try {
            userMigrationService.testConnection(source);
            io.println("Connection test passed.");
        } catch (Exception ex) {
            io.println("Connection test failed: " + ex.getMessage());
            return;
        }

        io.println("");
        io.println("[User module]");
        user.setEnabled(Boolean.TRUE);
        user.setStatusFilter(io.promptCsvIntegersWithDefault("Status filter", List.of(10)));
        user.setPasswordMode(selectPasswordMode());
        if (user.getPasswordMode() == UserPasswordMode.FIXED) {
            user.setFixedPassword(io.promptPassword("Fixed password"));
        } else {
            user.setFixedPassword(null);
        }
        user.setConflictPolicy(selectUsernameConflictPolicy());
        user.setEmailConflictPolicy(selectEmailConflictPolicy());
        user.setImportProfile(io.promptYesNo("Import personal_intro into profile", false));

        io.println("");
        io.println("[Execution]");
        execution.setBatchSize(io.promptIntWithDefault("Batch size", 500, 1));
        execution.setDryRun(io.promptYesNo("Dry run", true));
        execution.setFailFast(io.promptYesNo("Fail fast", false));
        execution.setIssueLimit(io.promptIntWithDefault("Issue sample limit", 200, 1));

        if (io.promptYesNo("Save profile", true)) {
            String defaultName = "migration-user-" + LocalDateTime.now().format(PROFILE_NAME_FORMATTER) + ".json";
            String fileName = io.promptWithDefault("Profile file name", defaultName);
            try {
                Path savedPath = profileStore.saveProfile(sanitizeForStorage(profile), fileName);
                io.println("Profile saved: " + savedPath);
            } catch (IOException ex) {
                io.println("Failed to save profile: " + ex.getMessage());
            }
        }

        if (io.promptYesNo("Run now", true)) {
            runProfile(profile);
        }
    }

    private void runFromSavedProfile() {
        List<Path> profiles;
        try {
            profiles = profileStore.listProfiles();
        } catch (IOException ex) {
            io.println("Failed to list profiles: " + ex.getMessage());
            return;
        }
        if (profiles.isEmpty()) {
            io.println("No saved profile found.");
            return;
        }

        io.println("");
        io.println("[Profiles]");
        for (int i = 0; i < profiles.size(); i++) {
            io.println((i + 1) + ") " + profiles.get(i).getFileName());
        }
        int choice = io.promptIntWithDefault("Select profile index", 1, 1);
        if (choice > profiles.size()) {
            io.println("Invalid profile index.");
            return;
        }

        MigrationProfile profile;
        try {
            profile = profileStore.loadProfile(profiles.get(choice - 1));
        } catch (IOException ex) {
            io.println("Failed to load profile: " + ex.getMessage());
            return;
        }

        if (profile.getSource() != null && !StringUtils.hasText(profile.getSource().getPassword())) {
            profile.getSource().setPassword(io.promptPassword("Source database password"));
        }
        if (profile.getModules() != null
                && profile.getModules().getUser() != null
                && profile.getModules().getUser().getPasswordMode() == UserPasswordMode.FIXED
                && !StringUtils.hasText(profile.getModules().getUser().getFixedPassword())) {
            profile.getModules().getUser().setFixedPassword(io.promptPassword("Fixed password"));
        }

        if (io.promptYesNo("Override dry run", false)) {
            profile.getExecution().setDryRun(io.promptYesNo("Dry run", true));
        }
        runProfile(profile);
    }

    private void runProfile(MigrationProfile profile) {
        try {
            UserMigrationReport report = userMigrationService.migrate(profile);
            Path reportPath = reportWriter.writeUserReport(report);
            io.println("Migration done.");
            io.println("Job ID: " + report.getJobId());
            io.println("Dry run: " + report.isDryRun());
            io.println("Source total: " + report.getSourceTotal());
            io.println("Processed: " + report.getProcessed());
            io.println("Inserted: " + report.getInserted());
            io.println("Updated: " + report.getUpdated());
            io.println("Skipped: " + report.getSkipped());
            io.println("Failed: " + report.getFailed());
            io.println("Report file: " + reportPath);
        } catch (Exception ex) {
            io.println("Migration failed: " + ex.getMessage());
        }
    }

    private UserPasswordMode selectPasswordMode() {
        io.println("Password mode: 1) fixed 2) random-unlogin");
        int choice = io.promptIntWithDefault("Select", 2, 1);
        return choice == 1 ? UserPasswordMode.FIXED : UserPasswordMode.RANDOM_UNLOGIN;
    }

    private MigrationProfile sanitizeForStorage(MigrationProfile profile) {
        MigrationProfile copy = new MigrationProfile();
        copy.getSource().setHost(profile.getSource().getHost());
        copy.getSource().setPort(profile.getSource().getPort());
        copy.getSource().setDb(profile.getSource().getDb());
        copy.getSource().setUser(profile.getSource().getUser());
        copy.getSource().setPassword(null);

        copy.getExecution().setBatchSize(profile.getExecution().getBatchSize());
        copy.getExecution().setDryRun(profile.getExecution().getDryRun());
        copy.getExecution().setFailFast(profile.getExecution().getFailFast());
        copy.getExecution().setIssueLimit(profile.getExecution().getIssueLimit());

        UserModuleConfig srcUser = profile.getModules().getUser();
        UserModuleConfig dstUser = copy.getModules().getUser();
        dstUser.setEnabled(srcUser.getEnabled());
        dstUser.setStatusFilter(srcUser.getStatusFilter() == null ? null : new ArrayList<>(srcUser.getStatusFilter()));
        dstUser.setPasswordMode(srcUser.getPasswordMode());
        dstUser.setFixedPassword(null);
        dstUser.setConflictPolicy(srcUser.getConflictPolicy());
        dstUser.setEmailConflictPolicy(srcUser.getEmailConflictPolicy());
        dstUser.setImportProfile(srcUser.getImportProfile());
        return copy;
    }

    private UsernameConflictPolicy selectUsernameConflictPolicy() {
        io.println("Username conflict policy: 1) skip 2) rename 3) overwrite-safe");
        int choice = io.promptIntWithDefault("Select", 1, 1);
        return switch (choice) {
            case 2 -> UsernameConflictPolicy.RENAME;
            case 3 -> UsernameConflictPolicy.OVERWRITE_SAFE;
            default -> UsernameConflictPolicy.SKIP;
        };
    }

    private EmailConflictPolicy selectEmailConflictPolicy() {
        io.println("Email conflict policy: 1) nullify 2) skip-user");
        int choice = io.promptIntWithDefault("Select", 1, 1);
        return choice == 2 ? EmailConflictPolicy.SKIP_USER : EmailConflictPolicy.NULLIFY;
    }
}
