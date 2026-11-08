"""手工冒烟：用与 DataInitializer 一致的演示数据跑一次求解。

运行：./.venv/Scripts/python.exe _smoke.py
更完整的断言见 test_algo.py。
"""

import time
from collections import defaultdict

from algo import schedule_courses

STUDENTS = [1, 2]


def course(cid, name, weeks, spw=2):
    return {
        "courseId": cid,
        "courseName": name,
        "studentCount": len(STUDENTS),
        "weeks": weeks,
        "sessionsPerWeek": spw,
        "studentIds": list(STUDENTS),
    }


courses = [
    course(1, "数据结构", 8),
    course(2, "计算机网络", 8),
    course(3, "操作系统", 8),
    course(4, "机器学习", 6),
]
labs = [
    {"labId": 1, "labName": "软件实验室", "capacity": 30},
    {"labId": 2, "labName": "网络实验室", "capacity": 30},
    {"labId": 3, "labName": "硬件实验室", "capacity": 24},
    {"labId": 4, "labName": "人工智能实验室", "capacity": 20},
]

start = time.time()
result = schedule_courses(courses, labs)
print("count", len(result), "elapsed", round(time.time() - start, 2))

days = defaultdict(set)
for item in result:
    days[(item["week"], item["day"])].add(item["courseId"])
print("第 1 周占用：", sorted({day: sorted(cids) for (week, day), cids in days.items() if week == 1}.items()))
for item in result[:8]:
    print(item)
