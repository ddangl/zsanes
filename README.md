# 麻醉科排班 + 考勤系统(Anesthesia Schedule)

面向麻醉科科室内部的排班与考勤管理系统:月度值班备班表导入、每日一键排班(值班/备班/接班/手术室分配)、排班工作台人工微调、周/月排班总览与导出;二期扩展外围排班、肝移植流转、请假/假期台账,三期统计报表。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router |
| 后端 | Spring Boot 3(Java 17)+ MyBatis-Plus |
| 数据库 | MySQL 8(utf8mb4) |
| 认证 | JWT(总值班 ADMIN / 普通成员 USER) |
| 导入导出 | EasyExcel(月度表导入、排班导出) |
| 部署 | 院内网 Web;docker-compose 一键部署 / 本地开发两种模式 |

## 目录结构

```
aneschedule/
├── README.md                  # 本文件
├── CHANGELOG.md               # 变更记录
├── .gitignore / .editorconfig
├── docs/                      # 项目文档
│   ├── 业务规则.md             # 需求整理(含 11 项待确认清单、5 条假设)
│   └── 实施规划.md             # 分期规划与一期详细设计
├── backend/                   # Spring Boot 后端
│   ├── pom.xml / Dockerfile
│   ├── mvnw / mvnw.cmd        # Maven Wrapper(本机无需装 Maven,仅需 JDK 17)
│   ├── src/main/java/com/anes/schedule/
│   │   ├── ScheduleApplication.java   # 启动入口
│   │   ├── config/            # 安全/JWT、MyBatis-Plus、CORS 等配置
│   │   ├── controller/        # REST API
│   │   ├── service/           # 业务逻辑
│   │   │   └── engine/        # ★ 排班引擎(规则管道,系统核心)
│   │   ├── mapper/            # MyBatis-Plus Mapper
│   │   ├── entity/            # 数据库实体
│   │   ├── dto/               # 请求/响应对象
│   │   └── common/            # 统一响应、异常处理、常量
│   ├── src/main/resources/
│   │   ├── application.yml    # 主配置(端口 8080,上下文 /api)
│   │   ├── application-dev.yml / application-prod.yml
│   │   ├── db/schema.sql      # 一期建表脚本(10 张核心表)
│   │   └── mapper/            # MyBatis XML(如需要)
│   └── src/test/java/         # 单元测试(排班引擎用例)
├── frontend/                  # Vue3 前端
│   ├── package.json / vite.config.ts / tsconfig.json
│   ├── Dockerfile / nginx.conf
│   └── src/
│       ├── main.ts / App.vue / router/
│       ├── api/               # axios 封装(request.ts)
│       ├── views/             # login / admin(档案·房间·参数) / roster(月度表)
│       │                      # / workbench(排班工作台) / overview(总览·我的)
│       ├── components/        # 排班矩阵表格等复用组件
│       ├── stores/            # Pinia
│       ├── types/             # TS 类型定义
│       ├── utils/             # 工具函数
│       └── assets/
├── scripts/                   # 开发辅助脚本(数据库初始化、数据导入等)
└── deploy/                    # 部署
    ├── docker-compose.yml     # mysql + backend + frontend(nginx)
    └── .env.example           # 环境变量模板(数据库密码等)
```

## 快速开始(本地开发)

前置:JDK 17 ✅、Node 18+ ✅;本机需 MySQL 8(未安装见下方"数据库")。

1. 建库:`mysql -uroot -p < backend/src/main/resources/db/schema.sql`
2. 后端:改 `backend/src/main/resources/application-dev.yml` 里的数据库账号密码,然后
   ```bash
   cd backend
   ./mvnw spring-boot:run        # Windows CMD 用 mvnw.cmd
   ```
3. 前端:
   ```bash
   cd frontend
   npm install
   npm run dev                   # http://localhost:5173,/api 自动代理到 8080
   ```

## 数据库

一期建表脚本在 [backend/src/main/resources/db/schema.sql](backend/src/main/resources/db/schema.sql),共 10 张核心表(账号/人员/亚专科/房间/月度表及明细/排班快照及明细/人员日状态/排班参数),表设计说明见 [docs/实施规划.md](docs/实施规划.md) 第 4.3 节。

> 本机无 MySQL 且无 Docker 时:安装 MySQL 8,或后续在装了 Docker 的部署机上用 `deploy/docker-compose.yml`(首次启动会自动执行 schema.sql 初始化)。

## 部署(院内网)

```bash
cd deploy
cp .env.example .env            # 修改数据库密码等
docker compose up -d --build    # 前端 80 端口,/api 反代到后端 8080
```

## 文档索引

| 文档 | 内容 |
|---|---|
| [docs/业务规则.md](docs/业务规则.md) | 需求整理:值班/备班/接班规则、手术室算法、肝移植状态机、请假假期、待确认清单(15 项)与假设 |
| [docs/实施规划.md](docs/实施规划.md) | 分期路线、一期详细设计(数据库/引擎/API/页面/规则中心 4.7/图示模块 4.8)、实施步骤、验收标准 |
| [docs/程序骨架.md](docs/程序骨架.md) | 施工图+填空手册:请求旅程、后端/前端骨架设计、样板模块(房间管理)、模块填空路线图、骨架验收清单 |
| [CHANGELOG.md](CHANGELOG.md) | 版本变更记录 |

## 开发进度(对应实施规划第 5 节)

- [x] 步骤 1:脚手架 + 目录结构 + schema
- [x] 步骤 2:认证 + 人员/房间/亚专科管理 + 规则中心(代码完成,单测与前后端构建全绿;登录与 CRUD 的运行时验证待 MySQL 环境,见 [scripts/dev-run.md](scripts/dev-run.md))
- [ ] 步骤 3:月度值班表 Excel 导入
- [ ] 步骤 4:排班引擎 + 单元测试
- [ ] 步骤 5:生成/调整/确认 + 排班工作台
- [ ] 步骤 6:周/月总览 + Excel 导出
- [ ] 步骤 7:图示模块(布局编辑器/画布模式/邻接软约束)
- [ ] 步骤 8:文档与部署
- [ ] 步骤 9:本地联调验收
