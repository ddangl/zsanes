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
