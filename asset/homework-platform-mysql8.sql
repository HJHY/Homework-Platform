SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `social_auth`;
CREATE TABLE `social_auth`
(
    `id`              INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `open_id`         VARCHAR(50) NOT NULL COMMENT 'open_id',
    `user_id`         INT         NOT NULL COMMENT '用户信息id',
    `login_type`      TINYINT(1)  NOT NULL COMMENT '登录类型',
    `ip_address`      VARCHAR(50) NULL     DEFAULT NULL COMMENT '用户登录ip',
    `ip_source`       VARCHAR(50) NULL     DEFAULT NULL COMMENT 'ip来源',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` DATETIME    NULL     DEFAULT NULL COMMENT '上次登录时间'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC,
    COMMENT '社交登录认证表';

DROP TABLE IF EXISTS user;
CREATE TABLE `user`
(
    `id`               INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户id',
    `username`         VARCHAR(32)  NOT NULL COMMENT '用户名',
    `realname`         VARCHAR(32)  NOT NULL COMMENT '真实姓名',
    `password`         VARCHAR(256) NULL COMMENT '密码',
    #对于使用第三方登录的用户来说,不一定会存在email
    `email`            VARCHAR(128) NULL UNIQUE COMMENT '电子邮箱',
    `sex`              VARCHAR(3)   NULL COMMENT '性别',
    `icon_url`         VARCHAR(256) NULL COMMENT '头像路径',
    `school_id`        VARCHAR(32)  NULL COMMENT '学校id',
    `student_id`       VARCHAR(32)  NULL COMMENT '学生id(学生学号)',
    `class_id`         INT          NULL COMMENT '学校班级id(指学生所在学校的班级id)',
    `is_disable`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否禁用',
    `is_valid`         BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '是否有效',
    `register_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `last_update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '用户信息表';


DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`
(
    `id`          INT         NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `role_name`   VARCHAR(20) NOT NULL COMMENT '角色名',
    `role_label`  VARCHAR(50) NOT NULL COMMENT '角色描述',
    `is_disable`  TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否禁用  0否 1是',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '角色表';

INSERT INTO `role` (`id`, `role_name`, `role_label`, `is_disable`)
VALUES (1, '超级管理员', '系统最高级管理员', 0);
INSERT INTO `role` (`id`, `role_name`, `role_label`, `is_disable`)
VALUES (2, '班级创建者', '班级创建者', 0);
INSERT INTO `role` (`id`, `role_name`, `role_label`, `is_disable`)
VALUES (3, '班级管理员', '班级管理员:班级成员中的具有管理权限的特殊成员', 0);
INSERT INTO `role` (`id`, `role_name`, `role_label`, `is_disable`)
VALUES (4, '班级成员', '班级普通成员', 0);


DROP TABLE IF EXISTS `clazz`;
CREATE TABLE `clazz`
(
    `class_id`         INT PRIMARY KEY AUTO_INCREMENT COMMENT '班级id',
    `class_name`       VARCHAR(32) NOT NULL COMMENT '班级名称',
    `description`      VARCHAR(256) DEFAULT '' COMMENT '班级描述',
    `creator_id`       INT         NOT NULL COMMENT '创建者id',
    `is_valid`         BOOLEAN      DEFAULT TRUE COMMENT '是否有效',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    #加上唯一联合索引(既有联合索引的特性,又有唯一索引的去重的特性)
    UNIQUE KEY `idx_creator_class_valid` (`creator_id`, `class_name`, `is_valid`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '班级表';


DROP TABLE IF EXISTS `user_class_role`;
CREATE TABLE `user_class_role`
(
    `id`               INT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    `user_id`          INT      NOT NULL DEFAULT -1 COMMENT '用户id',
    `class_id`         INT      NOT NULL DEFAULT -1 COMMENT '班级id',
    `role_id`          INT      NOT NULL DEFAULT -1 COMMENT '角色id',
    `join_time`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `is_valid` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有效',
    #加上唯一联合索引(既有联合索引的特性,又有唯一索引的去重的特性)
    UNIQUE INDEX `idx_user_class_role` (`user_id`, `class_id`, `role_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '用户班级角色表';

DROP TABLE IF EXISTS `homework_release`;
CREATE TABLE `homework_release`
(
    `homework_id`      INT PRIMARY KEY AUTO_INCREMENT COMMENT '发布的作业id',
    `class_id`         INT          NOT NULL COMMENT '班级id',
    `homework_name`    VARCHAR(256) COMMENT '作业名' DEFAULT '新作业',
    `creator_id`       INT          NOT NULL COMMENT '创建者',
    `end_time`         DATETIME     NOT NULL COMMENT '截止时间',
    `is_valid`         BOOLEAN      NOT NULL         DEFAULT TRUE COMMENT '是否有效',
    `description`      VARCHAR(256) NOT NULL         DEFAULT '' COMMENT '作业描述/备注',
    `launch_time`      DATETIME     NOT NULL         DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    #创建唯一联合索引来加速查询避免重复提交
    UNIQUE INDEX `idx_class_homework_valid` (`class_id`, `homework_name`, `is_valid`),
    INDEX `idx_creator_valid` (`creator_id`, `is_valid`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '作业发布表';


DROP TABLE IF EXISTS `homework_submission`;
CREATE TABLE `homework_submission`
(
    `homework_id`      INT PRIMARY KEY AUTO_INCREMENT COMMENT '提交的作业id',
    `user_id`          INT          NOT NULL COMMENT '用户id',
    `belonging_id`     INT          NOT NULL COMMENT '所属发布作业的id',
    `description`      VARCHAR(256) NOT NULL DEFAULT '' COMMENT '作业描述',
    `file_src`         VARCHAR(256) NOT NULL COMMENT '文件路径',
    `status`           INT          NOT NULL DEFAULT 0 COMMENT '状态0:签名已生成但是还未提交;状态1.已成功提交',
    `is_valid`         BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '是否有效',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '作业提交表';

DROP TABLE IF EXISTS `homework_reminder_message`;
CREATE TABLE `homework_reminder_message`
(
    `id`               INTEGER PRIMARY KEY AUTO_INCREMENT COMMENT '消息id',
    `user_id`          INT                NOT NULL COMMENT '用户id',
    `homework_id`      INT                NOT NULL COMMENT '作业id',
    `method`           INT      DEFAULT 1 NOT NULL COMMENT '通知方式:1.邮箱;2.微信;3.短信;4.QQ',
    `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deal_time`        DATETIME COMMENT '预计处理时间',
    `status`           INT      DEFAULT 0 NOT NULL COMMENT '状态:0为未处理,1为已处理',
    `last_update_time` DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间' #也表示最后扫描得时间
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '作业提醒消息表';


DROP TABLE IF EXISTS `push_setting`;
CREATE TABLE `push_setting`
(
    `id`           INT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    `user_id`      INT NOT NULL COMMENT '用户id',
    `advance_time` INT NOT NULL COMMENT '提前推送的时间(单位:h)',
    CONSTRAINT `unique_push_index` UNIQUE (`user_id`, `advance_time`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '推送设置表';


CREATE TABLE academy
(
    academy_id   INT PRIMARY KEY AUTO_INCREMENT COMMENT '学院id',
    academy_name VARCHAR(100) COMMENT '学院名称'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '学院表';
INSERT INTO academy (academy_id, academy_name)
VALUES (1, '电子与通信工程学院'),
       (2, '环境科学与工程学院'),
       (3, '地理科学与遥感学院'),
       (4, '教育学院（师范学院）'),
       (5, '音乐舞蹈学院'),
       (6, '体育学院'),
       (7, '外国语学院'),
       (8, '公共管理学院'),
       (9, '机械与电气工程学院'),
       (10, '生命科学学院'),
       (11, '经济与统计学院'),
       (12, '数学与信息科学学院'),
       (13, '管理学院'),
       (14, '化学化工学院'),
       (15, '新闻与传播学院'),
       (16, '计算机科学与网络工程学院'),
       (17, '法学院（律师学院）'),
       (18, '土木工程学院'),
       (20, '人文学院'),
       (21, '建筑与城市规划学院'),
       (22, '马克思主义学院');

CREATE TABLE major
(
    `major_id`          INT PRIMARY KEY AUTO_INCREMENT COMMENT '专业id',
    `major_name`        VARCHAR(100) COMMENT '专业名称',
    `academy_id`        INT COMMENT '学院id',
    `simple_major_name` VARCHAR(100) COMMENT '专业简称'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '专业表';

INSERT INTO major (`major_id`, `major_name`, `academy_id`, `simple_major_name`)
VALUES (1, '电子信息工程', 1, NULL),
       (2, '通信工程', 1, NULL),
       (3, '物联网工程', 1, NULL),
       (4, '环境科学', 2, NULL),
       (5, '环境工程', 2, NULL),
       (6, '地理科学', 3, NULL),
       (7, '地理信息科学', 3, NULL),
       (8, '遥感科学与技术', 3, NULL),
       (9, '人文地理与城乡规划', 3, NULL),
       (10, '小学教育', 4, NULL),
       (11, '学前教育', 4, NULL),
       (12, '教育技术学', 4, NULL),
       (13, '应用心理学', 4, NULL),
       (14, '特殊教育', 4, NULL),
       (15, '音乐学', 5, NULL),
       (16, '舞蹈编导', 5, NULL),
       (17, '体育教育(师范)', 6, NULL),
       (18, '社会体育指导与管理', 6, NULL),
       (19, '英语', 7, NULL),
       (20, '日语', 7, NULL),
       (21, '法语', 7, NULL),
       (22, '行政管理', 8, NULL),
       (23, '社会学', 8, NULL),
       (24, '机械设计制造及其自动化', 9, NULL),
       (25, '电气工程及其自动化', 9, NULL),
       (26, '智能制造工程', 9, NULL),
       (27, '机器人工程', 9, NULL),
       (28, '机器人工程', 10, NULL),
       (29, '机器人工程', 10, NULL),
       (30, '经济学', 11, NULL),
       (31, '国际经济与贸易', 11, NULL),
       (32, '金融学', 11, NULL),
       (33, '统计学', 11, NULL),
       (34, '数据科学与大数据技术', 11, NULL),
       (35, '数学与应用数学', 12, NULL),
       (36, '信息与计算科学', 12, NULL),
       (37, '工商管理', 13, NULL),
       (38, '工程管理', 13, NULL),
       (39, '物流管理', 13, NULL),
       (40, '电子商务', 13, NULL),
       (41, '会计学', 13, NULL),
       (42, '旅游管理', 13, NULL),
       (43, '旅游管理(中外合作办学)', 13, NULL),
       (44, '化学', 14, NULL),
       (45, '化学工程与工艺', 14, NULL),
       (46, '广播电视学', 15, NULL),
       (47, '网络与新媒体', 15, NULL),
       (48, '播音与主持艺术(普粤)', 15, NULL),
       (49, '广播电视编导', 15, NULL),
       (50, '计算机科学与技术', 16, '计科'),
       (51, '软件工程', 16, '软件'),
       (52, '网络工程', 16, '网络'),
       (53, '人工智能', 16, '智能'),
       (54, '网络空间安全', 16, '王安'),
       (55, '法学', 17, NULL),
       (56, '土木工程', 18, NULL),
       (57, '给排水科学与工程', 18, NULL),
       (58, '建筑环境与能源应用工程', 18, NULL),
       (61, '历史学', 20, NULL),
       (62, '汉语言文学', 20, NULL),
       (63, '建筑学', 21, NULL),
       (64, '城乡规划', 21, NULL),
       (65, '风景园林', 21, NULL),
       (66, '思想政治教育(师范)', 22, NULL),
       (67, '地理科学(师范)', 3, NULL),
       (68, '应用心理学(师范)', 4, NULL),
       (69, '音乐学(师范)', 5, NULL),
       (70, '英语(师范)', 7, NULL),
       (71, '数学与应用数学(师范)', 12, NULL),
       (72, '化学(师范)', 14, NULL),
       (73, '历史学(师范)', 20, NULL),
       (74, '汉语言文学(师范)', 20, NULL);

CREATE TABLE school_class
(
    `class_id`   INT PRIMARY KEY AUTO_INCREMENT COMMENT '班级id',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `major_id`   INT COMMENT '专业id'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC
    COMMENT '校内班级表';

INSERT INTO school_class (`class_id`, `class_name`, `major_id`)
VALUES (1, '网络191', 52),
       (2, '网络192', 52),
       (3, '网络193', 52),
       (4, '网络194', 52),
       (5, '软件191', 51),
       (6, '软件192', 51),
       (7, '软件193', 51),
       (8, '软件194', 51);