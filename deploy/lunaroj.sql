-- Auto-generated from docs/数据库设计.md
-- Generated at 2026-02-21 10:27:12

CREATE DATABASE IF NOT EXISTS `lunaroj`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE `lunaroj`;

CREATE TABLE `user` (
    `id`                   BIGINT UNSIGNED NOT NULL,
    `username`             VARCHAR(64)     NOT NULL COMMENT '登录用户名(唯一)',
    `password`             VARCHAR(128)    NOT NULL COMMENT '密码(BCrypt加盐)',
    `nickname`             VARCHAR(64)     NOT NULL COMMENT '昵称',
    `email`                VARCHAR(191)    NULL     COMMENT '邮箱',
    `email_verified`       BOOLEAN         NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证',
    `permission_group_id`  BIGINT UNSIGNED NOT NULL COMMENT '权限组ID',
    `profile`              TEXT            NULL     COMMENT '个人主页Markdown',
    `default_code_public`  BOOLEAN         NOT NULL DEFAULT 0 COMMENT '提交代码默认是否公开',
    `last_login_at`        DATETIME        NULL     COMMENT '最后登录时间',
    `created_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`           DATETIME        NULL COMMENT '软删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    INDEX `idx_permission_group` (`permission_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

ALTER TABLE `user`
  ADD COLUMN `email_active` VARCHAR(191)
    GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN `email` END) STORED,
  ADD UNIQUE KEY `uk_email_active` (`email_active`);

CREATE TABLE `permission_group` (
    `id`          BIGINT       UNSIGNED NOT NULL,
    `name`        VARCHAR(64)  NOT NULL COMMENT '权限组名称',
    `permissions` JSON         NULL     COMMENT '权限列表 ',
    `description` VARCHAR(255) NULL     COMMENT '描述',
    `is_built_in` BOOLEAN      NOT NULL DEFAULT 0 COMMENT '是否内置(内置不可删除)',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限组';

CREATE TABLE `notification` (
    `id`          BIGINT UNSIGNED NOT NULL,
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收用户',
    `type`        TINYINT       NOT NULL COMMENT '消息类型',
    `title`       VARCHAR(255)  NOT NULL,
    `content`     TEXT          NULL,
    `read_at`     DATETIME      NULL,
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME      NULL COMMENT '软删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_read_created` (`user_id`, `read_at`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息';

CREATE TABLE `user_attribute` (
    `user_id`    BIGINT UNSIGNED NOT NULL,
    `attr_key`   VARCHAR(64)     NOT NULL COMMENT '属性名',
    `attr_value` VARCHAR(191)    NOT NULL COMMENT '属性值',
    PRIMARY KEY (`user_id`, `attr_key`),
    UNIQUE KEY `uk_key_value` (`attr_key`, `attr_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展属性';

CREATE TABLE `tag_category` (
    `id`         BIGINT UNSIGNED NOT NULL,
    `name`       VARCHAR(16)  NOT NULL COMMENT '分类名',
    `sort_order` INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签分类';

CREATE TABLE `tag` (
    `id`          BIGINT UNSIGNED NOT NULL,
    `name`        VARCHAR(64)  NOT NULL COMMENT '标签名',
    `category_id` BIGINT  UNSIGNED NOT NULL COMMENT '所属分类',
    `sort_order`       INT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY uk_category_name (category_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';

CREATE TABLE `problem` (
    `id`                BIGINT UNSIGNED NOT NULL,
    `display_id`        VARCHAR(16)     NULL COMMENT '题目在后台管理的显示ID',
    `title`             VARCHAR(255)  NOT NULL COMMENT '题目标题',
    `description`       MEDIUMTEXT    NOT NULL COMMENT '题面Markdown/HTML',
    `input_description` TEXT          NULL     COMMENT '输入描述',
    `output_description` TEXT         NULL     COMMENT '输出描述',
    `samples`           JSON          NULL     COMMENT '样例列表',
    `note`              TEXT          NULL     COMMENT '提示',
    `time_limit`        INT           NOT NULL DEFAULT 1000 COMMENT '时间限制(ms)',
    `memory_limit`      INT           NOT NULL DEFAULT 262144 COMMENT '内存限制(KB), 默认256MB',
    `difficulty`        INT           NULL     COMMENT '难度, NULL为未设置',
    `difficulty_source` BOOLEAN       NOT NULL DEFAULT 0 COMMENT '难度来源 0-自动(AI) 1-手动设置',
    `solution`          MEDIUMTEXT    NULL     COMMENT '题解Markdown',
    `solution_visible`  BOOLEAN       NOT NULL DEFAULT 0 COMMENT '题解是否可见',
    `std_code`          MEDIUMTEXT    NULL     COMMENT '标程代码',
    `std_language`      TINYINT       NULL     COMMENT '标程语言 0-C++, 1-C, 2-Python, 3-Java, 4-txt',
    `std_visible`       BOOLEAN       NOT NULL DEFAULT 0 COMMENT '标程是否可见',
    `judge_mode`        TINYINT       NOT NULL DEFAULT 0 COMMENT '0-常规 1-SPJ 2-填空题 3-交互题',
    `scoring_config`    JSON          NULL COMMENT '测试点分值配置',
    `spj_code`          MEDIUMTEXT    NULL     COMMENT 'SPJ代码',
    `spj_language`      TINYINT       NULL     COMMENT 'SPJ语言',
    `accept_count`      INT           NOT NULL DEFAULT 0 COMMENT 'AC数',
    `submit_count`      INT           NOT NULL DEFAULT 0 COMMENT '提交数',
    
    `status`            TINYINT NOT NULL DEFAULT 0 COMMENT '0-草稿 1-待审核 2-已发布 3-已下架',
    `reviewed_by`       BIGINT UNSIGNED NULL COMMENT '审核题目者的用户ID',
    `reviewed_at`       DATETIME      NULL,
    `created_by`        BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`        BIGINT UNSIGNED NOT NULL,
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_by`        BIGINT UNSIGNED NULL,
    `deleted_at`        DATETIME      NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_display_id` (`display_id`),
    INDEX `idx_creator` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目';

CREATE TABLE `problem_tag` (
    `problem_id` BIGINT UNSIGNED NOT NULL,
    `tag_id`     BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`problem_id`, `tag_id`),
    INDEX `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-标签关联';

CREATE TABLE `testcase` (
    `id`              BIGINT UNSIGNED NOT NULL,
    `problem_id`      BIGINT UNSIGNED NOT NULL,
    `sort_order`      INT             NOT NULL DEFAULT 0,
    `input_path`      VARCHAR(255)    NULL     COMMENT '输入文件MinIO路径',
    `output_path`     VARCHAR(255)    NULL     COMMENT '输出文件MinIO路径, NULL表示待生成',
    `input_size`      BIGINT          NULL     COMMENT 'bytes',
    `output_size`     BIGINT          NULL     COMMENT 'bytes',
    `input_hash`      VARCHAR(64)     NULL     COMMENT '输入文件SHA-256',
    `output_hash`     VARCHAR(64)     NULL     COMMENT '输出文件SHA-256',
    `created_by`      BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
    `updated_by`      BIGINT UNSIGNED NOT NULL COMMENT '最后修改者用户ID',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_testcase` (`problem_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试点';

CREATE TABLE `problem_discussion` (
    `id`          BIGINT UNSIGNED NOT NULL,
    `problem_id`  BIGINT UNSIGNED NOT NULL COMMENT '关联题目ID',
    `contest_id`  BIGINT UNSIGNED NULL     COMMENT '关联比赛ID',
    `parent_id`   BIGINT UNSIGNED NULL     COMMENT 'NULL表示主帖，非NULL表示回复某主帖',
    `reply_to_id` BIGINT UNSIGNED NULL     COMMENT '被回复的楼层ID，用于二级回复@提示',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '发帖/回复用户',
    `title`       VARCHAR(255)    NULL     COMMENT '讨论标题，仅主帖有值',
    `content`     MEDIUMTEXT      NOT NULL COMMENT '内容Markdown',
    `is_pinned`   BOOLEAN         NOT NULL DEFAULT 0 COMMENT '是否置顶，仅主帖有效',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME        NULL,
    `deleted_by`  BIGINT UNSIGNED NULL     COMMENT '删除者(管理员或作者)',
    PRIMARY KEY (`id`),
    INDEX `idx_problem_parent` (`problem_id`, `parent_id`, `is_pinned` DESC, `created_at` DESC),
    INDEX `idx_contest_parent` (`contest_id`, `parent_id`, `created_at` DESC),
    INDEX `idx_user_created` (`user_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目讨论';

CREATE TABLE `submission` (
    `id`           BIGINT UNSIGNED NOT NULL,
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '提交用户ID',
    `problem_id`   BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    `problem_set_id` BIGINT UNSIGNED NULL   COMMENT '题库ID',
    `contest_id`   BIGINT UNSIGNED NULL     COMMENT '比赛ID，NULL表示练习提交',
    `code_length`  INT             NOT NULL COMMENT '代码字节数',
    `language`     TINYINT         NOT NULL COMMENT '语言 0-C++ 1-C 2-Python 3-Java 4-txt',
    `status`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0-WT0 1-WT1 2-CI 3-RI 4-AC 5-PE 6-WA 7-TLE 8-MLE 9-OLE 10-RE 11-CE 12-SE 13-NT',
    `score`        INT             NOT NULL DEFAULT 0 COMMENT '得分(OI模式，ACM模式恒为0)',
    `time_used`    INT             NULL     COMMENT '最大时间开销(ms)，评测完成前为NULL',
    `memory_used`  INT             NULL     COMMENT '最大内存开销(KB)，评测完成前为NULL',
    `pass_count`   INT             NOT NULL DEFAULT 0 COMMENT '通过测试点数',
    `total_count`  INT             NOT NULL DEFAULT 0 COMMENT '总测试点数',
    `is_public`    BOOLEAN         NOT NULL DEFAULT 0 COMMENT '代码是否公开',
    `judged_at`    DATETIME        NULL     COMMENT '评测完成时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_created`   (`user_id`, `created_at` DESC),
    INDEX `idx_problem_status` (`problem_id`, `status`, `created_at` DESC),
    INDEX `idx_problem_set_user` (`problem_set_id`, `user_id`, `created_at` DESC),
    INDEX `idx_contest_user` (`contest_id`, `user_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录';

CREATE TABLE `submission_code` (
    `submission_id`  BIGINT UNSIGNED NOT NULL,
    `code`           MEDIUMTEXT      NOT NULL COMMENT '提交代码',
    `compile_output` TEXT            NULL     COMMENT '编译器输出(CE时非NULL)',
    `case_results`   JSON            NULL     COMMENT '测试点结果列表，CE时为NULL',
    PRIMARY KEY (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交代码(垂直分表)';

CREATE TABLE `problem_set` (
    `id`             BIGINT UNSIGNED NOT NULL,
    `name`           VARCHAR(255)    NOT NULL COMMENT '题库名',
    `description`    TEXT            NULL     COMMENT '题库描述Markdown',
    `team_id`       BIGINT UNSIGNED NOT NULL COMMENT '所属小组ID',
    `testcase_view`  TINYINT         NOT NULL DEFAULT 1 COMMENT '测试点查看方式 0-不允许 1-截断部分 2-全部与下载',
    `display_format` TINYINT         NOT NULL DEFAULT 0 COMMENT '题目显示ID格式 0-字母(A-Z) 1-数字',
    `created_by`     BIGINT UNSIGNED NOT NULL,
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`     BIGINT UNSIGNED NOT NULL,
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     DATETIME        NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';

CREATE TABLE `problem_set_problem` (
    `problem_set_id` BIGINT UNSIGNED NOT NULL,
    `problem_id`     BIGINT UNSIGNED NOT NULL,
    `display_id`     VARCHAR(16)     NULL COMMENT '题目在该题库中的显示ID',
    `sort_order`     INT             NOT NULL DEFAULT 0 COMMENT '排列顺序',
    PRIMARY KEY (`problem_set_id`, `problem_id`),
    UNIQUE KEY `uk_set_display` (`problem_set_id`, `display_id`),
    INDEX `idx_problem` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库-题目关联';

CREATE TABLE `problem_set_tag` (
    `problem_set_id` BIGINT UNSIGNED NOT NULL,
    `tag_id`         BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`problem_set_id`, `tag_id`),
    INDEX `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库-标签关联';

CREATE TABLE `contest` (
    `id`                  BIGINT UNSIGNED NOT NULL,
    `display_id`          VARCHAR(32)     NULL     COMMENT '用户可见的比赛ID，NULL表示未发布',
    `title`               VARCHAR(255)    NOT NULL COMMENT '比赛标题',
    `description`         TEXT            NULL     COMMENT '比赛描述',
    `problem_set_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联题库ID',
    `team_id`            BIGINT UNSIGNED NOT NULL COMMENT '所属小组ID',
    `mode`                TINYINT         NOT NULL DEFAULT 0 COMMENT '赛制 0-ACM 1-OI 2-IOI 3-练习',
    `start_at`            DATETIME        NOT NULL COMMENT '开始时间',
    `end_at`              DATETIME        NOT NULL COMMENT '结束时间',
    `freeze_at`           DATETIME        NULL     COMMENT '封榜时间，NULL表示不封榜',
    `unfreeze_at`         DATETIME        NULL     COMMENT '解榜时间，NULL表示不自动解榜',
    `penalty`             INT             NOT NULL DEFAULT 20 COMMENT '罚时(分钟)，0表示无罚时',
    `access_mode`         TINYINT         NOT NULL DEFAULT 0 COMMENT '准入方式 0-组内默认 1-邀请码 2-管理员分配',
    `visibility`          TINYINT         NOT NULL DEFAULT 2 COMMENT '可见性 0-隐藏 1-组内私有 2-组内公开',
    `rank_visible`        BOOLEAN         NOT NULL DEFAULT 1 COMMENT '赛时榜单是否对参赛者可见',
    `submit_info_visible` BOOLEAN         NOT NULL DEFAULT 1 COMMENT '赛时运行内存与时间是否对参赛者可见',
    `judge_priority`      TINYINT         NOT NULL DEFAULT 2 COMMENT '判题优先级(越低越好)',
    `invite_code`         VARCHAR(64)     NULL COMMENT '邀请码',
    `announcements`       JSON            NULL COMMENT '比赛公告列表',
    `enable_clar`         BOOLEAN         NOT NULL DEFAULT 1 COMMENT '是否开启答疑服务',
    `enable_balloon`      BOOLEAN         NOT NULL DEFAULT 1 COMMENT '是否开启气球服务',
    `enable_print`     BOOLEAN         NOT NULL DEFAULT 1 COMMENT '是否开启打印服务',
    `created_by`          BIGINT UNSIGNED NOT NULL,
    `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`          BIGINT UNSIGNED NOT NULL,
    `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`          DATETIME        NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_display_id` (`team_id`, `display_id`),
    INDEX `idx_team_start` (`team_id`, `start_at` DESC),
    INDEX `idx_problem_set` (`problem_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛';

CREATE TABLE `contest_participant` (
    `contest_id`   BIGINT UNSIGNED NOT NULL,
    `user_id`      BIGINT UNSIGNED NOT NULL,
    `joined_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `signed_out_at` DATETIME       NULL     COMMENT '签退时间，NULL表示未签退',
    PRIMARY KEY (`contest_id`, `user_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛参赛者';

CREATE TABLE `contest_clarification` (
    `id`          BIGINT UNSIGNED NOT NULL,
    `contest_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属比赛ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '提问者',
    `problem_id`  BIGINT UNSIGNED NULL     COMMENT '关联题目ID',
    `question`    TEXT            NOT NULL COMMENT '问题内容',
    `answer`      TEXT            NULL     COMMENT '回复内容，NULL表示未回复',
    `is_public`   BOOLEAN         NOT NULL DEFAULT 0 COMMENT '是否公开(所有参赛者可见)',
    `answered_by` BIGINT UNSIGNED NULL     COMMENT '回复者(比赛管理员)',
    `answered_at` DATETIME        NULL     COMMENT '回复时间',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_contest_created` (`contest_id`, `created_at` DESC),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛答疑';

CREATE TABLE `contest_balloon` (
    `id`          BIGINT UNSIGNED NOT NULL,
    `contest_id`  BIGINT UNSIGNED NOT NULL,
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '过题用户',
    `problem_id`  BIGINT UNSIGNED NOT NULL COMMENT '过题题目',
    `submission_id` BIGINT UNSIGNED NOT NULL COMMENT '首次AC的提交ID',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0-待处理 1-处理中 2-已完成',
    `handler`     VARCHAR(64)     NULL     COMMENT '处理者标识(气球机客户端ID)',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handled_at`  DATETIME        NULL     COMMENT '处理完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_user_problem` (`contest_id`, `user_id`, `problem_id`),
    INDEX `idx_contest_status` (`contest_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='气球请求';

CREATE TABLE `contest_print` (
    `id`          BIGINT UNSIGNED NOT NULL,
    `contest_id`  BIGINT UNSIGNED NOT NULL,
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '请求打印的用户',
    `content`     MEDIUMTEXT      NOT NULL COMMENT '打印内容',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0-待处理 1-处理中 2-已完成',
    `handler`     VARCHAR(64)     NULL     COMMENT '处理者标识(打印机客户端ID)',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handled_at`  DATETIME        NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_contest_status` (`contest_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打印请求';

CREATE TABLE `team` (
    `id`           BIGINT UNSIGNED NOT NULL,
    `name`         VARCHAR(64)     NOT NULL COMMENT '小组名',
    `description`  TEXT            NULL     COMMENT '小组描述',
    `join_mode`    TINYINT         NOT NULL DEFAULT 0 COMMENT '加入方式 0-自由加入 1-邀请码 2-批准加入',
    `visibility`   TINYINT         NOT NULL DEFAULT 0 COMMENT '可见方式 0-仅成员可见 1-所有人可见',
    `invite_code`  VARCHAR(32)     NULL     COMMENT '邀请码',
    `is_built_in`  BOOLEAN         NOT NULL DEFAULT 0 COMMENT '是否内置(不可删除)',
    `created_by`   BIGINT UNSIGNED NOT NULL COMMENT '创建者(组长)',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`   BIGINT UNSIGNED NOT NULL,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`   DATETIME        NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组';

ALTER TABLE `team`
  ADD COLUMN `name_active` VARCHAR(64)
    GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN `name` END) STORED,
  ADD UNIQUE KEY `uk_name_active` (`name_active`);

CREATE TABLE `team_member` (
    `team_id`   BIGINT UNSIGNED NOT NULL,
    `user_id`    BIGINT UNSIGNED NOT NULL,
    `role`       TINYINT         NOT NULL DEFAULT 2 COMMENT '角色 0-组长 1-小组管理员 2-普通成员 3-待批准',
    `joined_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_by` BIGINT UNSIGNED NULL,
    `deleted_at` DATETIME        NULL,
    PRIMARY KEY (`team_id`, `user_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组成员';

CREATE TABLE `system_config` (
    `config_key`   VARCHAR(64)     NOT NULL COMMENT '配置项键名',
    `config_value` VARCHAR(1024)   NULL     COMMENT '配置项值',
    `description`  VARCHAR(255)    NULL     COMMENT '配置说明',
    `updated_by`   BIGINT UNSIGNED NULL     COMMENT '最后修改者，NULL表示系统初始化',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局配置';

CREATE TABLE `announcement` (
    `id`         BIGINT UNSIGNED NOT NULL,
    `title`      VARCHAR(255)    NOT NULL COMMENT '公告标题',
    `content`    MEDIUMTEXT      NOT NULL COMMENT '公告内容Markdown',
    `is_visible` BOOLEAN         NOT NULL DEFAULT 1 COMMENT '是否可见',
    `created_by` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT UNSIGNED NOT NULL,
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME        NULL COMMENT '软删除',
    PRIMARY KEY (`id`),
    INDEX `idx_visible_created` (`is_visible`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局公告';

