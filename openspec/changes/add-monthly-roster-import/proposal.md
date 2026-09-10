# add-monthly-roster-import · Proposal

## Why

排班引擎(实施步骤 4)必须有结构化的月度值班备班表数据才能运转,而月表目前由总值班每月用 Excel 手工维护,旧系统只能部分自动读取(副麻仅 8 人),其余靠手动调整。本变更是引擎的最短前置数据通路:把真实月表 Excel 读准、存对。

设计依据:已批准的设计文档 `docs/designs/monthly-roster-import.md`(office-hours 问诊 + 三轮对抗性评审 9/10)。

## What Changes

- 新增月度值班备班表**导入能力**(最小数据通路,方案 A):
  - 两阶段接口:`POST /roster/parse`(仅解析,返回行集合+校验问题清单)→ `POST /roster/import`(前端回传修正后完整行集合,服务端复核入库;阻断项整体拒绝、零部分提交)
  - 解析器适配科室**现有**月表格式(合并单元格/多 sheet),禁止静默丢弃无法映射区域;复杂表预留 POI 直读通道(传递依赖,无需新增)
  - 10 类岗位(position_type)落库:值班 4 档(ON_CALL_CHIEF/T2/T3/T4)与 STANDBY1/STANDBY2/LONG_RELIEF/ASSISTANT_RELIEF/ASSISTANT_MID 按 duty_date 当日固定,SHORT_RELIEF 按 seq 顺序池;三类待样本校准项先按默认约定
  - 同月重传整月替换(事务删重插,幂等=业务数据等价);导入即 ACTIVE
  - 前端 MonthlyRosterView 改造:上传(选目标月)→ 预览(阻断/警告分级、人名候选修正白名单)→ 入库;仅 ADMIN 可操作
- **不做**(显式出范围):在线表格编辑(方案 B,等真实用户反馈)、模板下载(适配现有格式的推论)、排班逻辑(步骤 4)

## Capabilities

### New Capabilities

- `monthly-roster`: 月度值班备班表的解析、校验、导入与查询——把科室现有 Excel 月表变成引擎可消费的结构化数据(按日固定安排 + 顺序池双形态),支持同月重传替换。

### Modified Capabilities

(无——openspec/specs 目前为空,本变更为项目首个能力)

## Impact

- 后端:新增 roster 模块(RosterController/RosterParseService/RosterImportService、MonthlyRoster/MonthlyRosterItem 实体与 Mapper、解析适配层);复用既有 common 底座与 staff 工号匹配规则;EasyExcel 已在依赖中
- 前端:`MonthlyRosterView.vue` 由占位改造为导入工作页;api/types 增 roster 组
- 数据库:**无 schema 变更**(monthly_roster/monthly_roster_item 两表已就绪)
- 下游:月表替换不自动失效已生成排班快照,工作台提示"月表已更新,建议重新生成"(一期一句话)
