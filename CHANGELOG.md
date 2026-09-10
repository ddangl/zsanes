# 变更记录(Changelog)

本文件记录项目显著变更,格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/),
版本号遵循语义化版本(SemVer)。

## [Unreleased]

### Added
- 项目文档:业务规则整理稿(v0.1,含 11 项待确认清单与 5 条假设)、实施规划(v0.1,分三期)。
- 工程骨架:后端(Spring Boot 3 / Java 17 / MyBatis-Plus)、前端(Vue3 + Vite + TS + Element Plus)、
  一期数据库建表脚本(10 张核心表)、docker-compose 部署配置、Maven Wrapper。

### Changed
- 业务规则 v0.2:按 2026年8月总人员信息表(418 人)核对修订——新增 2.1 人员档案实况(职称分布、
  双亚专科、同名 8 对以工号为主键、带教/规培年级/副麻级别数据缺口、周期性缺席 6 条);
  "剔除主任"精确为 2 人;待确认 #1/#3 更新并新增 #12;新增假设 A6。
- 实施规划:规模修正为 418 人;staff 表字段按实况修订(双亚专科、行政职位、周期性缺席字段)。
- 手术室房间实况落库:7 区域 89 间种子数据写入 schema.sql(眼科 67/68、心外科 11–20 标记
  schedulable=0 排除排班);room 表新增 schedulable 字段;星标暂无信息源缓做、咨询岗确认占房间;
  待确认 #1 关闭,新增 #13(外围新岗位配置)/#14(16号楼 21 号房、备班1 与 8号楼4楼口径),假设 A2 已确认。
- 实施规划 v0.2:新增 4.7「规则中心」设计(双形态规则=结构化参数+自然语言原文、RuleRegistry
  策略挂载、rule_version 版本快照、MANUAL_REMINDER 提醒卡兜底、schedule_config 并入后废弃);
  一期范围/数据库表/实施步骤/验收标准同步更新;业务规则 v0.3 新增第 12 节规则中心映射说明。
- 图示模块(布局可视化)设计定稿入规划 v0.3:新增 4.8(room_layout/room_relation 两表、SVG 自绘
  编辑器 + 底图描摹、邻接关系几何自动推导 + 手动修正、工作台画布模式按主麻染色与劈叉预警、
  OR.ADJACENCY_PREF 软约束);实施步骤插入步骤 7,原 7/8 顺延;业务规则 v0.4 新增空间邻接偏好
  条目与待确认 #15。
- 新增 docs/程序骨架.md v1.0(施工图+填空手册):请求旅程总览、后端公共底座(统一响应/错误码/
  JWT 认证/Swagger)与样板模块(房间管理五件套)、RuleHandler/ScheduleStep 扩展点、前端主布局/
  路由守卫/axios 封装升级/样板页面、7 模块填空路线图、骨架验收清单。本轮仅文档,骨架代码待实施。
- 实施步骤 2(分支 feat/step2-admin):认证 + 人员/房间/亚专科管理 + 规则中心。
  后端:公共底座(ApiResponse/错误码/全局异常/ThreadLocal 认证上下文/JWT 拦截器)、
  Swagger(springdoc)、三个业务模块与 Excel 导入(工号 upsert、带教关系转换、同名提示)、
  规则中心(双形态实体/发布版本化/RuleRegistry+MANUAL_REMINDER 示例 Handler);
  schema v0.2:staff 按实况修订、rule_definition/rule_version 两表、schedule_config 移除、
  admin 种子与 25 条规则种子。前端:MainLayout 角色菜单/路由守卫/axios 拦截/auth store、
  房间(样板 CRUD)、亚专科、人员(含导入弹窗)、规则中心(编辑+JSON 校验+回读预览+发布+版本)。
  验证:后端单测 7/7 绿(含 8 月表 418 行实测解析与职称分布核对),前端 type-check/build 通过;
  运行时 E2E 待 MySQL 环境(scripts/dev-run.md)。

### Fixed(本地部署 E2E 实测发现)
- schema.sql:`year_month` 为 MySQL 保留字导致建表失败 → 更名 `roster_month`。
- application.yml:重复的顶层 `spring:` 键(DuplicateKeyException)→ 合并为单一块。
- JDBC URL:`characterEncoding=utf8mb4` 非法(Java 字符集名)→ 改 `UTF-8`(dev/prod)。
- RoomService.areas():`GROUP BY area` 配 `ORDER BY sort` 触发 ONLY_FULL_GROUP_BY → 改按 area 排序。
- E2E 结果(绿色版 MySQL + 浏览器实测):登录/角色菜单/房间89间分页筛选/人员418人搜索
  (陆珠凤"只在肝科"正确显示)/8月表API导入418人+自动建6个亚专科/规则25条+中文回读预览 全部通过。
- 实施步骤 3(分支 feat/roster-import,openspec add-monthly-roster-import + TDD):
  月度值班备班表导入最小数据通路。TDD 六循环 42 单测全绿(岗位映射10类/月份闰年规则/
  人员匹配链/校验分级/POI解析适配层/实体组装);服务与 /roster 接口(parse零写入、
  import复核整体拒绝零部分提交、同月重传幂等替换、按月查询);
  前端导入页(预览校验/同名候选与日期白名单修正/阻断禁入/当月双Tab视图)。
  E2E 13/13:合并单元格跨日展开、南克同名双候选、原样导入400问题清单、
  修正后33条入库、重导幂等、28按日+5顺序池查询、401权限;UI 渲染验证通过。
  待办:真实月表样本校准适配层(tasks 6.2)与免修正率基线。
