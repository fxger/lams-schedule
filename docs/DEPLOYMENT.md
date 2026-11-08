# 部署与运行

## 环境要求

| 组件 | 要求 | 本机实测 |
| --- | --- | --- |
| JDK | 17 | 17.0.10 |
| Maven | 3.9+（首次编译需联网下载依赖） | 3.9.9 |
| Node.js | 18+ | 22.22.2 / npm 10.9.7 |
| Python | 3.10+（CI 使用 3.13） | 3.13.7 |

## 目录与端口

| 服务 | 目录 | 端口 | 配置位置 |
| --- | --- | --- | --- |
| 算法服务 | `algo/` | 8000 | 启动命令 `--port`，后端侧 `lams.schedule.algo-url` |
| 后端 | `backend/` | 8080 | `application.yml` 的 `server.port` |
| 前端 | `frontend/` | 8081 | `vue.config.js` 的 `devServer.port` |

修改端口时，三处要一起调整：前端代理目标、后端 `lams.cors.allowed-origins`、算法服务启动参数与 `algo-url`。

## 依赖安装

```powershell
cd srp_schedule\algo
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt

cd srp_schedule\frontend
npm install
```

后端依赖由 Maven 在首次 `mvn` 命令时自动解析，无需额外安装。

> 上述三条依赖安装命令**未在本机重新执行**：本机已有 `.venv`（Python 3.13.7）与 `node_modules`，`requirements.txt` 和 `package-lock.json` 的版本取自该实测环境。

## 配置

配置文件只有一个：`backend/src/main/resources/application.yml`（未拆分 dev/prod profile，也未引入自动加载 `.env` 的机制）。

| 项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:h2:file:./data/lamsdb;...NON_KEYWORDS=DAY,WEEK,VALUE` | H2 文件库；`NON_KEYWORDS` 是为 H2 2.x 保留字准备的 |
| `spring.sql.init.mode` | `always` | 每次启动执行幂等的 `schema.sql` |
| `lams.jwt.secret-key` | `${LAMS_JWT_SECRET:演示密钥}` | **对外部署必须覆盖** |
| `lams.jwt.ttl` | `86400000` | 24 小时 |
| `lams.schedule.algo-url` | `http://localhost:8000/api/schedule` | 求解服务地址 |
| `lams.schedule.timeout-ms` | `180000` | 需大于算法侧 60s 求解上限 |
| `spring.h2.console.enabled` | `true` | 本地调试用，对外必须 `false` |

设置环境变量（`.env.example` 只是变量清单，项目**不会自动读取 `.env`**）：

```powershell
$env:LAMS_JWT_SECRET = "替换为随机长字符串"
```

```bash
export LAMS_JWT_SECRET="替换为随机长字符串"
```

## 启动顺序

必须按「算法 → 后端 → 前端」启动，否则自动排课会提示无法连接算法服务。

```powershell
# 终端 1
cd srp_schedule\algo
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000

# 终端 2
cd srp_schedule\backend
mvn -DskipTests spring-boot:run

# 终端 3
cd srp_schedule\frontend
npm run serve
```

访问 `http://localhost:8081/`。首次启动会自动建表并写入演示数据（只在表为空时）。

## 验证清单

```powershell
cd srp_schedule\algo
.\.venv\Scripts\python.exe -m unittest test_algo -v
.\.venv\Scripts\python.exe _smoke.py

cd srp_schedule\backend
mvn -B test

cd srp_schedule\frontend
npm run lint
npm run build
```

2026-11-08 在 Windows 本机的实测结果：

| 命令 | 结果 |
| --- | --- |
| `mvn -B test` | BUILD SUCCESS，4 tests / 0 failures |
| `python -m unittest test_algo -v` | 9 tests OK（1.47s） |
| `npm run lint` | No lint errors found |
| `npm run build` | Build complete，113.9s；1.39 MiB bundle 体积警告（非失败） |
| `GET :8000/health` / `:8000/docs` | 200 / 200 |
| `POST :8080/auth/login` | `code=0`，签发 JWT |
| `GET :8080/users/students`（无 token） | 401 |
| `GET :8081/` | 200 |
| `POST /schedule/auto` → 轮询 | `COMPLETED` / 100% / 60 条记录；再次提交同样成功 |

## 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 排课任务 `FAILED`，提示无法连接算法服务 | 算法服务未启动或端口不是 8000；核对 `lams.schedule.algo-url`。 |
| `Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"` | H2 2.x 不允许两者共存，连接串已去掉后者。 |
| 启动报 `Table "SCHEDULES" not found` | `spring.sql.init.mode` 被改成了 `never`，保持 `always`。 |
| 算法服务返回 422，提示 `studentIds` 缺失 | `POST /api/schedule` 的课程对象必须带 `studentIds`；后端 `PythonAlgoClient` 会自动从 `course_students` 查询并下传。 |
| 前端接口 401 | token 过期或没登录，重新登录；默认有效期 24 小时。 |
| 端口被占用 | 修改对应配置。若在受限沙箱/托管终端中后端被注入了随机端口，可显式指定：`mvn -DskipTests spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"`（本机实测有效）。 |
| 想清空数据重来 | **先备份**：停掉后端，再删除 `backend/data/` 目录，重启即重新建表与写入种子数据。这会丢失本地全部排课结果。 |

## 对外部署前必须处理

本项目定位是本地可运行的教学/演示项目，直接暴露到公网并不安全。至少完成：

1. 设置强随机的 `LAMS_JWT_SECRET`，不要使用 `application.yml` 里的演示默认值。
2. 关闭 `spring.h2.console.enabled`，并把 `/h2-console/**` 从 JWT 白名单中移除。
3. 更换或删除演示账号（`admin` / `student1` / `student2`，弱密码 `123456`）。
4. 收紧 `lams.cors.allowed-origins`，只允许真实前端域名。
5. 用真实数据库替换单机 H2 文件库，并为任务状态引入持久化（当前存内存，重启丢失）。
6. 为算法服务增加鉴权（当前 `/api/schedule` 无鉴权，仅由内网/本机可达性保护）。
