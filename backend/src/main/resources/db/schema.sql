-- =====================================================================
-- 麻醉科排班 + 考勤系统 · 一期建表脚本(schema v0.2,步骤2落地版)
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
-- 3. 人员档案(字段按 2026-08 实况,见 docs/业务规则.md 2.1)
--    job_role 实际取值:主麻 / 总值班 / 本院住院 / 麻护 / 规培 / 进修 / 轮转
--    ("预备"表头存在但 8 月无人;职称/职位是两列:job_role 职称、title 行政职位)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS staff (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  emp_no         VARCHAR(50)  NULL COMMENT '工号(唯一键,导入匹配以此为准)',
  name           VARCHAR(50)  NOT NULL,
  job_role       VARCHAR(20)  NOT NULL COMMENT '主麻/总值班/本院住院/麻护/规培/进修/轮转/预备',
  title          VARCHAR(20)  NULL COMMENT '行政职位:主任/副主任(仅主任剔除排班)',
  sub_level      TINYINT      NULL COMMENT '副麻级别1~5(8月表无此数据,待确认#3)',
  specialty1_id  BIGINT       NULL COMMENT '第一亚专科',
  specialty2_id  BIGINT       NULL COMMENT '第二亚专科',
  mentor_id      BIGINT       NULL COMMENT '带教上级id,自关联;导入时由"教师行挂学生工号"转换',
  grade          VARCHAR(20)  NULL COMMENT '规培年级(1/2/3,8月表未填)',
  saturday_work  TINYINT(1)   NULL COMMENT '周六是否上班(NULL未标注/0否/1是)',
  part_time_rule VARCHAR(100) NULL COMMENT '周期性缺席(如:每周四金山;待确认#12)',
  note           VARCHAR(200) NULL COMMENT '特殊说明原文',
  phone          VARCHAR(20)  NULL,
  active         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '在职/停用',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_emp_no (emp_no),
  KEY idx_specialty1 (specialty1_id),
  KEY idx_mentor (mentor_id),
  KEY idx_job_role (job_role)
) ENGINE = InnoDB COMMENT = '人员档案';

-- ---------------------------------------------------------------------
-- 4. 手术室房间
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS room (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  code        VARCHAR(20) NOT NULL COMMENT '房间号,如 22、802',
  area        VARCHAR(50) NULL COMMENT '区域,如 8号楼4楼、16号楼肝外科',
  starred     TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '星标术间:配更高级别副麻(信息源暂缺,一期无数据)',
  schedulable TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否参与本科排班:0=眼科(无全麻需求)/心外科(单独管理)',
  sort        INT         NOT NULL DEFAULT 0,
  active      TINYINT(1)  NOT NULL DEFAULT 1,
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
  rule_snapshot JSON       NULL COMMENT '生成时生效的规则版本集[{code,version}]',
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
  rule_code        VARCHAR(60) NULL COMMENT '该岗位由哪条规则产生(rule_definition.rule_code)',
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
-- 10. 规则中心(结构化参数 payload + 自然语言原文 nl_text 双形态,见实施规划 4.7)
--     原 schedule_config 已废弃,配置全部迁为规则
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rule_definition (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  rule_code      VARCHAR(60)  NOT NULL COMMENT '规则编码,如 OR.FIXED_ASSIGN_LU',
  category       VARCHAR(20)  NOT NULL COMMENT 'DUTY/OR/STAFF_AVAILABILITY/PERIPHERAL/LEAVE/GENERAL',
  name           VARCHAR(100) NOT NULL,
  nl_text        TEXT         NOT NULL COMMENT '科室原话(自然语言原文,存档溯源)',
  payload        JSON         NOT NULL COMMENT '结构化参数(引擎执行,RuleHandler 解释)',
  rule_type      VARCHAR(40)  NOT NULL COMMENT '引擎挂载类型,对应 RuleHandler.ruleType()',
  status         VARCHAR(10)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/RETIRED',
  priority       INT          NOT NULL DEFAULT 100,
  effective_from DATE         NULL,
  effective_to   DATE         NULL,
  version        INT          NOT NULL DEFAULT 0 COMMENT '已发布版本号',
  updated_by     BIGINT       NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule_code (rule_code),
  KEY idx_category (category),
  KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '规则中心-规则定义';

CREATE TABLE IF NOT EXISTS rule_version (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  rule_id      BIGINT   NOT NULL,
  version      INT      NOT NULL,
  nl_text      TEXT     NOT NULL,
  payload      JSON     NOT NULL,
  published_by BIGINT   NULL,
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule_version (rule_id, version)
) ENGINE = InnoDB COMMENT = '规则中心-版本快照';

-- ---------------------------------------------------------------------
-- 初始种子:管理员账号(admin / anes@2026,BCrypt;首次登录后请修改)
-- ---------------------------------------------------------------------
INSERT INTO sys_user (username, password_hash, role) VALUES
('admin', '$2b$10$DJ0Cwi7hvZHaSzEcMCgMde0ioCsSxoPjP01B6AjeN6hrqHPQWdOcu', 'ADMIN')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- ---------------------------------------------------------------------
-- 初始规则 25 条(nl_text 均为科室访谈原文,见 docs/业务规则.md;status=ACTIVE)
-- ---------------------------------------------------------------------
INSERT INTO rule_definition (rule_code, category, name, nl_text, payload, rule_type, status, priority) VALUES
('DUTY.ONCALL_COMPOSITION', 'DUTY', '值班组构成', '值班,不能休息,最先放入排班表,第二天自动休息 1老总+1二档+1三档+1四档',
 '{"chief":1,"tier2":1,"tier3":1,"tier4":1,"cannotRest":true,"firstPlaced":true}', 'ONCALL_COMPOSITION', 'ACTIVE', 10),
('DUTY.ONCALL_NEXT_DAY_REST', 'DUTY', '值班次日自动休息', '值班人员第二天自动休息',
 '{"restType":"REST_AFTER_CALL","autoMark":true}', 'NEXT_DAY_REST', 'ACTIVE', 11),
('DUTY.STANDBY1', 'DUTY', '备班1', '备班1每天一换(尽量不能休息,如换班,手动修改),8号楼4楼手术室其中两间,安排手术时,第一个安排',
 '{"rotateDaily":true,"area":"8号楼4楼手术室","firstSurgery":true,"note":"占整层还是两间待确认#14"}', 'STANDBY1', 'ACTIVE', 12),
('DUTY.SHORT_RELIEF_ORDER', 'DUTY', '短接班顺延', '短接班按照接班顺序依次排(如遇换班,休息,手动修改),备班1大于短接班,备班1跟短接班重叠时,下一个短接班人员顶上',
 '{"orderSequential":true,"skipIfStandby1Conflict":true}', 'SHORT_RELIEF', 'ACTIVE', 13),
('DUTY.LONG_RELIEF', 'DUTY', '长接班', '长接班,不能休息',
 '{"cannotRest":true,"directJoin":true}', 'LONG_RELIEF', 'ACTIVE', 14),
('DUTY.ASSISTANT_RELIEF_COPY', 'DUTY', '副麻接班沿用前日', '副麻接班,礼拜一到礼拜四之间读取前一天的排班数据(改动工程浩大,故现在只能自动读取8个副麻人员,其余手动修改),礼拜五重置',
 '{"copyPreviousDay":["MON","TUE","WED","THU"],"resetOn":"FRI","note":"新系统全量自动,不受8人限制(假设A4)"}', 'ASSISTANT_RELIEF', 'ACTIVE', 15),
('DUTY.STANDBY2', 'DUTY', '备班2正常排班', '备2不用考虑排班,可以休息,未加入当日值班备班表,正常排班',
 '{"joinNormalScheduling":true,"restAllowed":true}', 'STANDBY2', 'ACTIVE', 16),
('OR.EXCLUDE_DIRECTOR', 'OR', '剔除主任', '剔除主任(2026-08核对:仅2人——仓静、缪长虹;22位副主任职称均为"主麻",照常参与排班)',
 '{"excludeTitles":["主任"]}', 'EXCLUDE_DIRECTOR', 'ACTIVE', 20),
('OR.FIXED_ASSIGN_LU', 'OR', '固定分配:陆珠凤', '优先安排陆珠凤22-29(已确认:16号楼肝外科手术室21-29中的22-29段,21号房归属待确认#14)',
 '{"staffName":"陆珠凤","roomFrom":22,"roomTo":29,"area":"16号楼肝外科手术室"}', 'FIXED_ASSIGN', 'ACTIVE', 21),
('OR.FIXED_ASSIGN_STANDBY1', 'OR', '固定分配:备班1', '安排备班1 802-825(已确认:802-825即8号楼4楼手术室整层;与"占其中两间"口径差异见待确认#14)',
 '{"position":"STANDBY1","roomFrom":802,"roomTo":825,"area":"8号楼4楼手术室"}', 'FIXED_ASSIGN', 'ACTIVE', 22),
('OR.SPECIAL_SINGLE_ROOMS', 'OR', '特殊单间人员与预留数', '预留特殊人员(单间)的房间数,张燕影、苏子敏、葛宁花、备班、咨询看情况1间/2间(咨询岗确认占房间)',
 '{"persons":["张燕影","苏子敏","葛宁花"],"reservedCountDefault":1,"reservedCountMax":2}', 'SPECIAL_ROOMS', 'ACTIVE', 23),
('OR.LEAD_TWO_ROOMS', 'OR', '主麻两间制与亚专科优先', '随机选择一名主麻(做两间)进行排班,主麻有亚专科的人员安排到对应的科室,直到剩余房间数为预留的数量;空间邻接偏好:肩并肩>面对面>背对背,劈叉尽量避免(待确认#15)',
 '{"roomsPerLead":2,"specialtyFirst":true,"adjacencyPref":{"sideBySide":3,"faceToFace":2,"backToBack":1,"splitPenalty":5,"enabled":true}}', 'LEAD_ROOMS', 'ACTIVE', 24),
('OR.MENTOR_PAIRING_FIRST', 'OR', '带教副麻优先配对', '优先给有带教学生的主麻分配其带教副麻,分配完成后,没有副麻的房间随机安排副麻',
 '{"studentFollowsMentorFirst":true,"othersRandom":true}', 'MENTOR_PAIRING', 'ACTIVE', 25),
('OR.SENIOR_BEFORE_JUNIOR', 'OR', '先排大再排小', '两间房的先排大再排小,有带教的排了一间剩余一间给大,单间的人员直接给小(大=本院/规培3年级,小=规培1,2年级/3年级麻护/进修)',
 '{"order":["SENIOR_ASSIST","JUNIOR_ASSIST"],"singleRoomGets":"JUNIOR_ASSIST"}', 'SENIOR_FIRST', 'ACTIVE', 26),
('OR.MAX_RETRY', 'OR', '引擎整体重试上限', '遇到排班房间或人员无法安排时,将再次自动排班',
 '{"maxRetry":20}', 'MAX_RETRY', 'ACTIVE', 27),
('OR.STAR_ROOM', 'OR', '星标术间(缓做)', '星标术间安排更好的副麻(暂拉不到术间信息,一期缓做、字段保留)',
 '{"enabled":false,"reason":"术间信息源暂缺,待可用后启用"}', 'STAR_ROOM', 'RETIRED', 28),
('SA.LIMIN_HALF_DAY', 'STAFF_AVAILABILITY', '李敏:半天班', '李敏只上半天班,周六不上班(8月表唯一标注周六否)',
 '{"staffName":"李敏","halfDay":true,"saturdayWork":false}', 'STAFF_AVAILABILITY', 'ACTIVE', 30),
('SA.ZHANGXIAOGUANG_JINSHAN', 'STAFF_AVAILABILITY', '张晓光:每周四金山', '张晓光(每周4金山)',
 '{"staffName":"张晓光","weeklyAbsent":["THU"],"location":"金山"}', 'STAFF_AVAILABILITY', 'ACTIVE', 31),
('SA.ZHONGJING_WUSONG', 'STAFF_AVAILABILITY', '钟静:每周四吴淞', '钟静(每周四吴淞)',
 '{"staffName":"钟静","weeklyAbsent":["THU"],"location":"吴淞"}', 'STAFF_AVAILABILITY', 'ACTIVE', 32),
('SA.FANGHAO_THU_ONLY', 'STAFF_AVAILABILITY', '方浩:周四来', '方浩(周4来)',
 '{"staffName":"方浩","workdaysOnly":["THU"]}', 'STAFF_AVAILABILITY', 'ACTIVE', 33),
('SA.SUZIMIN_35', 'STAFF_AVAILABILITY', '苏子敏:3、5来', '苏子敏(3,5来)',
 '{"staffName":"苏子敏","workdaysOnly":["WED","FRI"]}', 'STAFF_AVAILABILITY', 'ACTIVE', 34),
('SA.LUZHUFENG_HEPATIC_ONLY', 'STAFF_AVAILABILITY', '陆珠凤:只在肝科', '陆珠凤(只在肝科)',
 '{"staffName":"陆珠凤","onlyArea":"16号楼肝外科手术室"}', 'STAFF_AVAILABILITY', 'ACTIVE', 35),
('GEN.MEETING_NO_DEDUCT', 'GENERAL', '会议/教学不扣假', '会议、教学类的记录,其实也是单位的工作,只是不参与科室的常规排班,所以不扣假期(确认:当天不参与排班,不扣假期)',
 '{"joinSchedule":false,"deductLeave":false,"dayStatus":"MEETING"}', 'DAY_STATUS', 'ACTIVE', 40),
('GEN.SPECIAL_CASE_EXCLUDE', 'GENERAL', '特殊情况=手动排除', '特殊情况也表示不参与常规排班的人员……并非不参与排班,可以认为是一个手动排除的方法,不是休息,方便总值班在HIS上进行手动调整(确认)',
 '{"dayStatus":"MANUAL_EXCLUDE","isRest":false,"deductLeave":false}', 'DAY_STATUS', 'ACTIVE', 41),
('GEN.MANUAL_REMIND_EXPERT_CLINIC', 'GENERAL', '提醒:专家门诊需手动调整', '专家门诊安排1主麻,特殊:星期一,二,四(按照超哥给予的顺序名单安排,但是因为是半天的,可能需要老总手动调整)',
 '{"reminder":"专家门诊按名单排,半天制,可能需老总手动调整","weekdays":["MON","TUE","THU"]}', 'MANUAL_REMINDER', 'ACTIVE', 42)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- ---------------------------------------------------------------------
-- 11. 手术室房间种子数据(2026-09-09 科室提供,共 89 间)
--     schedulable=0:67/68 眼科(无全麻需求)、11-20 心外科(单独管理)
-- ---------------------------------------------------------------------
INSERT INTO room (code, area, schedulable, sort) VALUES
('802', '8号楼4楼手术室', 1, 1)
,('803', '8号楼4楼手术室', 1, 2)
,('804', '8号楼4楼手术室', 1, 3)
,('805', '8号楼4楼手术室', 1, 4)
,('806', '8号楼4楼手术室', 1, 5)
,('807', '8号楼4楼手术室', 1, 6)
,('808', '8号楼4楼手术室', 1, 7)
,('809', '8号楼4楼手术室', 1, 8)
,('810', '8号楼4楼手术室', 1, 9)
,('811', '8号楼4楼手术室', 1, 10)
,('812', '8号楼4楼手术室', 1, 11)
,('813', '8号楼4楼手术室', 1, 12)
,('814', '8号楼4楼手术室', 1, 13)
,('815', '8号楼4楼手术室', 1, 14)
,('816', '8号楼4楼手术室', 1, 15)
,('817', '8号楼4楼手术室', 1, 16)
,('818', '8号楼4楼手术室', 1, 17)
,('819', '8号楼4楼手术室', 1, 18)
,('820', '8号楼4楼手术室', 1, 19)
,('821', '8号楼4楼手术室', 1, 20)
,('822', '8号楼4楼手术室', 1, 21)
,('823', '8号楼4楼手术室', 1, 22)
,('824', '8号楼4楼手术室', 1, 23)
,('825', '8号楼4楼手术室', 1, 24)
,('826', '8号楼3楼手术室', 1, 25)
,('827', '8号楼3楼手术室', 1, 26)
,('828', '8号楼3楼手术室', 1, 27)
,('829', '8号楼3楼手术室', 1, 28)
,('830', '8号楼3楼手术室', 1, 29)
,('831', '8号楼3楼手术室', 1, 30)
,('832', '8号楼3楼手术室', 1, 31)
,('833', '8号楼3楼手术室', 1, 32)
,('834', '8号楼3楼手术室', 1, 33)
,('835', '8号楼3楼手术室', 1, 34)
,('836', '8号楼3楼手术室', 1, 35)
,('837', '8号楼3楼手术室', 1, 36)
,('838', '8号楼3楼手术室', 1, 37)
,('839', '8号楼3楼手术室', 1, 38)
,('840', '8号楼3楼手术室', 1, 39)
,('841', '8号楼3楼手术室', 1, 40)
,('842', '8号楼3楼手术室', 1, 41)
,('843', '8号楼3楼手术室', 1, 42)
,('844', '8号楼3楼手术室', 1, 43)
,('845', '8号楼3楼手术室', 1, 44)
,('61', '10号楼4楼手术室', 1, 45)
,('62', '10号楼4楼手术室', 1, 46)
,('63', '10号楼4楼手术室', 1, 47)
,('64', '10号楼4楼手术室', 1, 48)
,('65', '10号楼4楼手术室', 1, 49)
,('66', '10号楼4楼手术室', 1, 50)
,('67', '10号楼4楼手术室', 0, 51)
,('68', '10号楼4楼手术室', 0, 52)
,('69', '10号楼4楼手术室', 1, 53)
,('70', '10号楼4楼手术室', 1, 54)
,('71', '10号楼4楼手术室', 1, 55)
,('72', '10号楼4楼手术室', 1, 56)
,('74', '10号楼3楼手术室', 1, 57)
,('75', '10号楼3楼手术室', 1, 58)
,('76', '10号楼3楼手术室', 1, 59)
,('77', '10号楼3楼手术室', 1, 60)
,('78', '10号楼3楼手术室', 1, 61)
,('79', '10号楼3楼手术室', 1, 62)
,('80', '10号楼3楼手术室', 1, 63)
,('81', '10号楼3楼手术室', 1, 64)
,('82', '10号楼3楼手术室', 1, 65)
,('51', '21号楼手术室', 1, 66)
,('52', '21号楼手术室', 1, 67)
,('53', '21号楼手术室', 1, 68)
,('55', '21号楼手术室', 1, 69)
,('56', '21号楼手术室', 1, 70)
,('21', '16号楼肝外科手术室', 1, 71)
,('22', '16号楼肝外科手术室', 1, 72)
,('23', '16号楼肝外科手术室', 1, 73)
,('24', '16号楼肝外科手术室', 1, 74)
,('25', '16号楼肝外科手术室', 1, 75)
,('26', '16号楼肝外科手术室', 1, 76)
,('27', '16号楼肝外科手术室', 1, 77)
,('28', '16号楼肝外科手术室', 1, 78)
,('29', '16号楼肝外科手术室', 1, 79)
,('11', '心外科手术室', 0, 80)
,('12', '心外科手术室', 0, 81)
,('13', '心外科手术室', 0, 82)
,('14', '心外科手术室', 0, 83)
,('15', '心外科手术室', 0, 84)
,('16', '心外科手术室', 0, 85)
,('17', '心外科手术室', 0, 86)
,('18', '心外科手术室', 0, 87)
,('19', '心外科手术室', 0, 88)
,('20', '心外科手术室', 0, 89)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
