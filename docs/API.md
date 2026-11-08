# 接口清单

## 通用约定

- 后端所有业务接口返回统一结构：`{ "code": 0, "msg": null, "data": ..., "timestamp": ... }`；`code != 0` 为业务失败，前端 axios 拦截器会弹出 `msg`。
- 除 `/auth/login`、`/error`、`/h2-console/**` 外，后端接口都要求请求头 `Authorization: Bearer <token>`。未携带或校验失败返回 HTTP 401（已实测）。
- 前端通过 `vue.config.js` 的 devServer 代理访问后端，路径与后端一致；以下路径均以前端视角书写。
- 算法服务是独立进程，**无鉴权**，仅由本机/内网可达性保护。

## 认证与用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/auth/login` | 登录。请求体 `{ username, password, role }`，`role` 为 `admin` / `student` 且必须与账号一致；成功返回 `data.token` |
| `GET` | `/users/students` | 学生列表，供管理员给课程分配学生 |

## 课程

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/courses` | 课程列表 |
| `GET` | `/courses/{id}` | 课程详情 |
| `POST` | `/courses` | 新增课程 |
| `PUT` | `/courses/{id}` | 修改课程 |
| `DELETE` | `/courses/{id}` | 删除课程（逻辑删除，`status=0`） |

课程关键字段：`weeks`（排课周数）、`periods`（每周连续节数）、`studentIds`（选课学生）。

## 实验室

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/labs` | 实验室列表 |
| `POST` | `/labs` | 新增实验室 |
| `PUT` | `/labs/{id}` | 修改实验室 |
| `DELETE` | `/labs/{id}` | 删除实验室 |

实验室关键字段：`capacity`（参与容量约束）。

## 排课

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/schedule?week=N` | 管理员按周查看全量课表，`week` 为 1–16 |
| `GET` | `/schedule/mine?week=N` | 学生查看本人课表，`week` 缺省为 1 |
| `POST` | `/schedule` | 手工新增一条排课 |
| `PUT` | `/schedule/{id}` | 修改一条排课 |
| `DELETE` | `/schedule/{id}` | 删除一条排课 |
| `DELETE` | `/schedule/all` | 清空全部排课 |
| `POST` | `/schedule/auto` | 触发自动排课，返回 `data` 为 `taskId` |
| `GET` | `/schedule/task-status/{taskId}` | 查询任务状态：`status`（`RUNNING` / `COMPLETED` / `FAILED` / `NOT_FOUND`）、`progress`、`message`、`total` |

自动排课是异步的：`POST /schedule/auto` 立刻返回 `taskId`，随后由前端每 3 秒轮询状态。

## 算法服务（FastAPI, :8000）

启动后访问 `http://127.0.0.1:8000/docs` 查看交互式文档（已实测返回 200）。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/health` | 返回 `{"status":"ok"}` |
| `POST` | `/api/schedule` | 求解，返回每节次一条记录的数组 |

### `POST /api/schedule` 请求体

```json
{
  "courses": [
    {
      "courseId": 1,
      "courseName": "数据结构",
      "studentCount": 2,
      "weeks": 8,
      "sessionsPerWeek": 2,
      "studentIds": [1, 2]
    }
  ],
  "labs": [
    { "labId": 1, "labName": "软件实验室", "capacity": 30 }
  ]
}
```

`studentIds` **必填**（缺省会让求解器静默跳过「学生不撞课」约束）。缺字段返回 HTTP 422，`detail` 中带具体字段路径；输入非法或无可行解返回 HTTP 400 并给出中文诊断。

响应元素：

```json
{
  "courseId": 1,
  "courseName": "数据结构",
  "week": 1,
  "day": 1,
  "session": 1,
  "labId": 1,
  "labName": "软件实验室"
}
```

一个连续块（例如 `sessionsPerWeek=2`）会展开成相邻 `session` 的多条记录。

## 调用示例

```bash
# 算法服务健康检查
curl http://127.0.0.1:8000/health

# 登录（后端）
curl -X POST http://127.0.0.1:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","role":"admin"}'

# 触发自动排课（把 <TOKEN> 换成登录返回的 data.token）
curl -X POST http://127.0.0.1:8080/schedule/auto \
  -H "Authorization: Bearer <TOKEN>"

# 查询任务状态
curl http://127.0.0.1:8080/schedule/task-status/<TASK_ID> \
  -H "Authorization: Bearer <TOKEN>"
```

> 演示账号仅用于本地环境，请勿在对外服务中使用。
