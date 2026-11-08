"""FastAPI 排课服务：POST /api/schedule"""

import logging
from typing import List

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from algo import schedule_courses

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger("schedule")

app = FastAPI(title="LAMS Schedule Service")


class CourseDTO(BaseModel):
    courseId: int
    courseName: str
    studentCount: int
    weeks: int
    sessionsPerWeek: int
    # 选课学生 ID 列表，用于保证同一学生不会在同一节次被安排多门课。
    # 必填：缺省会让求解器静默跳过学生互斥约束，产出学生撞课的课表。
    studentIds: List[int]


class LabDTO(BaseModel):
    labId: int
    labName: str
    capacity: int


class ScheduleRequestVO(BaseModel):
    courses: List[CourseDTO]
    labs: List[LabDTO]


class ScheduleResultDTO(BaseModel):
    courseId: int
    courseName: str
    week: int
    day: int
    session: int
    labId: int
    labName: str


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    fields = "；".join(
        f"{'.'.join(str(part) for part in err.get('loc', ()))}: {err.get('msg', '')}"
        for err in exc.errors()
    )
    logger.error("请求数据校验失败: %s", fields)
    return JSONResponse(status_code=422, content={"detail": "请求数据格式错误：" + fields})


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/api/schedule", response_model=List[ScheduleResultDTO])
def generate_schedule(req: ScheduleRequestVO):
    courses = [course.model_dump() for course in req.courses]
    labs = [lab.model_dump() for lab in req.labs]
    logger.info("收到排课请求：课程 %d 门，实验室 %d 间", len(courses), len(labs))

    try:
        result = schedule_courses(courses, labs)
    except ValueError as exc:
        logger.warning("排课失败: %s", exc)
        return JSONResponse(status_code=400, content={"detail": str(exc)})

    logger.info("排课完成，共生成 %d 条记录", len(result))
    return result
