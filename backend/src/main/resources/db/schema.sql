-- =====================================================================
-- 麻醉科排班 + 考勤系统 · 一期建表脚本(schema v0.1)
-- MySQL 8+,utf8mb4;表设计说明见 docs/实施规划.md 第 4.3 节
-- 约定:不用外键约束(便于数据维护),关联靠应用层保证;
--       排除"肝移植/外围"二期表,一期仅在相关表预留 position_type 扩展值
-- =====================================================================

CREATE DATABASE IF NOT EXISTS anes_schedule
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE anes_schedule;

-- ---------------------------------------------------------------------
-- 1. 登录账号
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希(BCrypt)',
  staff_id      BIGINT       NULL COMMENT '关联人员档案',
  role          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN=总值班,USER=普通成员',
  enabled       TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_staff (staff_id)
) ENGINE = InnoDB COMMENT = '登录账号';

-- ---------------------------------------------------------------------
-- 2. 亚专科
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS specialty (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  name       VARCHAR(50) NOT NULL COMMENT '亚专科名称,如心胸外科、神外',
  sort       INT         NOT NULL DEFAULT 0,
  active     TINYINT(1)  NOT NULL DEFAULT 1,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE = InnoDB COMMENT = '亚专科';

-- ---------------------------------------------------------------------
-- 3. 人员档案
--    job_role: DIRECTOR主任 / CHIEF主麻(可任总值班) / RESERVE预备 /
--              FULL本院 / TRAINEE_Y3|Y2|Y1规培3/2/1年级 /
--              NURSE麻护 / FELLOW进修 / ROTATOR轮转
--    sub_level: 副麻级别 1~5(假设 A1:5 最高,1 最低),可空
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staff (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  emp_no       VARCHAR(50) NULL COMMENT '工号',
  name         VARCHAR(50) NOT NULL,
  job_role     VARCHAR(20) NOT NULL,
  sub_level    TINYINT     NULL COMMENT '副麻级别1~5',
  specialty_id BIGINT      NULL COMMENT '亚专科',
  mentor_id    BIGINT      NULL COMMENT '带教上级(主麻)id,自关联',
  title        VARCHAR(50) NULL COMMENT '职称(主任医师等,展示用)',
  phone        VARCHAR(20) NULL,
  active       TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '在职/停用',
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_emp_no (emp_no),
  KEY idx_specialty (specialty_id),
  KEY idx_mentor (mentor_id),
  KEY idx_job_role (job_role)
) ENGINE = InnoDB COMMENT = '人员档案';

-- ---------------------------------------------------------------------
-- 4. 手术室房间
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS room (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  code       VARCHAR(20) NOT NULL COMMENT '房间号,如 22、802',
  area       VARCHAR(50) NULL COMMENT '区域,如 8号楼4楼',
  starred    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '星标术间:配更高级别副麻',
  sort       INT         NOT NULL DEFAULT 0,
  active     TINYINT(1)  NOT NULL DEFAULT 1,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = '手术室房间';

-- ---------------------------------------------------------------------
-- 5. 月度值班备班表(总值班每月上传一次)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monthly_roster (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  year_month CHAR(7)     NOT NULL COMMENT '月份,如 2026-09',
  status     VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE',
  created_by BIGINT      NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_year_month (year_month)
) ENGINE = InnoDB COMMENT = '月度值班备班表';

-- 6. 月度表明细
--    有 duty_date = 当日固定安排(值班各档、备班1、备班2)
--    duty_date 为 NULL + seq = 顺序池(短接班按序顺延取人)
--    position_type: ON_CALL_CHIEF值班老总 / ON_CALL_T2|T3|T4 二三四档 /
--      LONG_RELIEF长接班 / SHORT_RELIEF短接班 / STANDBY1备班1 /
--      STANDBY2备班2 / ASSISTANT_RELIEF副麻接班 / ASSISTANT_MID副麻中班
CREATE TABLE IF NOT EXISTS monthly_roster_item (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  roster_id     BIGINT      NOT NULL,
  duty_date     DATE        NULL,
  position_type VARCHAR(30) NOT NULL,
  seq           INT         NOT NULL DEFAULT 0 COMMENT '顺序池序号,从1开始',
  staff_id      BIGINT      NOT NULL,
  PRIMARY KEY (id),
  KEY idx_roster (roster_id),
  KEY idx_date (duty_date)
) ENGINE = InnoDB COMMENT = '月度值班备班表明细';

-- ---------------------------------------------------------------------
-- 7. 人员×日期状态
--    type: REST_AFTER_CALL 值班次日自动休息 / MANUAL_EXCLUDE 手动排除(特殊情况) /
--          MEETING 会议教学(不扣假;二期请假模块接入后扩展)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staff_day_status (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id    BIGINT       NOT NULL,
  status_date DATE         NOT NULL,
  type        VARCHAR(30)  NOT NULL,
  note        VARCHAR(200) NULL,
  created_by  BIGINT       NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_staff_date (staff_id, status_date)
) ENGINE = InnoDB COMMENT = '人员日状态(休息/排除/会议)';

-- ---------------------------------------------------------------------
-- 8. 每日排班快照
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedule (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  duty_date    DATE        NOT NULL,
  version      INT         NOT NULL DEFAULT 1 COMMENT '同日重新生成时递增',
  seed         BIGINT      NOT NULL COMMENT '随机种子,同数据同seed结果可复现',
  status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/CONFIRMED已确认',
  fail_reason  VARCHAR(500) NULL COMMENT '生成失败原因(缺人/缺房明细)',
  generated_at DATETIME    NULL,
  confirmed_at DATETIME    NULL,
  confirmed_by BIGINT      NULL,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_date_version (duty_date, version)
) ENGINE = InnoDB COMMENT = '每日排班快照';

-- 9. 排班明细
--    position_type: 值班(ON_CALL_*)/STANDBY1/STANDBY2/LONG_RELIEF/SHORT_RELIEF/
--      ASSISTANT_RELIEF/ASSISTANT_MID 之外,手术室侧为
--      LEAD_ANES主麻 / SENIOR_ASSIST大 / JUNIOR_ASSIST小 / SPECIAL单独间
--    role_tag: 带教/单间特殊 等展示标签;source: AUTO引擎 / MANUAL手动
--    一人一天一岗,由唯一键保证(肝移植二期独立成表,不受此约束)
CREATE TABLE IF NOT EXISTS schedule_assignment (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  schedule_id      BIGINT      NOT NULL,
  staff_id         BIGINT      NOT NULL,
  position_type    VARCHAR(30) NOT NULL,
  room_id          BIGINT      NULL,
  role_tag         VARCHAR(20) NULL,
  source           VARCHAR(10) NOT NULL DEFAULT 'AUTO',
  before_snapshot  JSON        NULL COMMENT '手动修改留痕:改前内容',
  updated_by       BIGINT      NULL,
  created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_schedule_staff (schedule_id, staff_id),
  KEY idx_schedule (schedule_id),
  KEY idx_room (room_id)
) ENGINE = InnoDB COMMENT = '排班明细';

-- ---------------------------------------------------------------------
-- 10. 排班参数配置(需求待确认项全部数据化,见业务规则.md 假设 A1~A5)
--     示例键:
--       FIXED_ASSIGNMENTS      固定房间分配 [{"staffName":"陆珠凤","roomFrom":22,"roomTo":29},
--                                             {"position":"STANDBY1","roomFrom":802,"roomTo":825}]
--       SPECIAL_SINGLE_ROOMS   特殊单间人员名单 ["张燕影","苏子敏","葛宁花",...]
--       RESERVED_ROOM_COUNT    特殊人员预留间数 {"default":1}
--       ENGINE_MAX_RETRY       引擎整体重试上限(默认 20)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedule_config (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  config_key   VARCHAR(50)  NOT NULL,
  config_value JSON         NOT NULL,
  description  VARCHAR(200) NULL,
  updated_by   BIGINT       NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB COMMENT = '排班参数配置';

-- ---------------------------------------------------------------------
-- 初始参数(可按科室确认结果调整,更新配置无需改代码)
-- ---------------------------------------------------------------------
INSERT INTO schedule_config (config_key, config_value, description) VALUES
('FIXED_ASSIGNMENTS', '[
  {"staffName":"陆珠凤","roomFrom":22,"roomTo":29},
  {"position":"STANDBY1","roomFrom":802,"roomTo":825}
]', '手术室固定房间分配(假设A2:22-29/802-825为固定房间段)'),
('SPECIAL_SINGLE_ROOMS', '["张燕影","苏子敏","葛宁花"]', '特殊单间人员名单(备班/咨询视情况预留,见预留间数)'),
('RESERVED_ROOM_COUNT', '{"default":1,"备注":"视情况1~2间,待确认"}', '特殊人员预留单间数'),
('ENGINE_MAX_RETRY', '20', '排班引擎整体重试上限')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
