# 本地开发与运行指南(步骤 2 之后)

## 前置

| 工具 | 要求 | 状态 |
|---|---|---|
| JDK | 17+ | ✅ 本机 17.0.19 |
| Node | 18+ | ✅ 本机 18.20.8 |
| MySQL | 8.0 | ⚠️ 本机未安装(见下) |
| Maven | 无需(用 backend/mvnw 自举) | ✅ |

## 1. 数据库(本机没有 MySQL 时的两条路)

**路 A:本机安装 MySQL 8** → 执行建表脚本(含 89 间房、25 条规则、admin 账号种子):

```bash
mysql -uroot -p < backend/src/main/resources/db/schema.sql
```

**路 B:部署机上用 docker-compose**(见 `deploy/README 说明`,首次启动自动执行 schema.sql)。

建好后修改 `backend/src/main/resources/application-dev.yml` 的数据库账号密码。

## 2. 启动后端(端口 8080,上下文 /api)

```bash
cd backend
./mvnw spring-boot:run        # Windows CMD 用 mvnw.cmd
```

- Swagger 接口文档:http://localhost:8080/api/swagger-ui.html
- 初始账号:`admin / anes@2026`(BCrypt,首登后请修改)

## 3. 启动前端(端口 5173,/api 自动代理到 8080)

```bash
cd frontend
npm install                   # 首次
npm run dev                   # http://localhost:5173
```

## 4. 运行时验证清单(对应程序骨架.md 第 5 节)

1. [ ] 登录页:admin/anes@2026 登录成功进入主布局;错误密码提示"用户名或密码错误"
2. [ ] 手术室房间:7 区域 89 间已就绪;新增重复房号被拦截并提示;行内"参与排班"开关即时保存
3. [ ] 亚专科:CRUD 正常
4. [ ] 人员档案:点"Excel 导入"→ 下载模板 → 上传《2026年8月总人员信息表.xlsx》
   → 报告应显示 总行数 418、新增 418、同名提示 8 条左右、自动新建亚专科 6 个
5. [ ] 规则中心:25 条种子规则为 ACTIVE;任选一条 → 编辑 → "回读预览"显示中文参数;
   "发布"版本 +1;版本历史可查
6. [ ] 未登录直接访问 http://localhost:5173 → 跳登录页
7. [ ] 用浏览器开发者工具删掉 localStorage 的 token 后操作 → 弹"未登录"并跳回登录页

## 5. 常用命令

```bash
cd backend && ./mvnw test            # 后端单测(含 8 月表导入解析实测)
cd frontend && npm run type-check    # 前端类型检查
cd frontend && npm run build         # 前端构建
```
