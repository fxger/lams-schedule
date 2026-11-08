"""排课求解的约束断言。

运行：./.venv/Scripts/python.exe -m unittest test_algo -v
"""

import unittest
from collections import defaultdict

from algo import DAYS, SECTIONS, _diagnose, schedule_courses
from main import CourseDTO


def course(cid, name, weeks, spw, students, count=None):
    return {
        "courseId": cid,
        "courseName": name,
        "studentCount": len(students) if count is None else count,
        "weeks": weeks,
        "sessionsPerWeek": spw,
        "studentIds": list(students),
    }


def make_labs(n=4, capacity=30):
    return [
        {"labId": i, "labName": f"实验室{i}", "capacity": capacity}
        for i in range(1, n + 1)
    ]


class ScheduleConstraintsTest(unittest.TestCase):
    """所有排课结果都必须满足的硬约束。"""

    def assert_valid(self, result, courses, lab_list):
        by_id = {c["courseId"]: c for c in courses}
        lab_by_id = {lab["labId"]: lab for lab in lab_list}
        student_courses = defaultdict(set)
        for c in courses:
            for sid in c["studentIds"]:
                student_courses[sid].add(c["courseId"])

        expected = sum(c["weeks"] * c["sessionsPerWeek"] for c in courses)
        self.assertEqual(len(result), expected, "总节数应等于各课程 weeks × sessionsPerWeek 之和")

        lab_slots = set()
        slot_courses = defaultdict(set)
        blocks = defaultdict(list)
        for item in result:
            self.assertIn(item["courseId"], by_id)
            self.assertIn(item["labId"], lab_by_id)
            self.assertTrue(1 <= item["week"] <= 16, "周次越界")
            self.assertTrue(1 <= item["day"] <= DAYS, "星期越界")
            self.assertTrue(1 <= item["session"] <= SECTIONS, "节次越界")
            self.assertGreaterEqual(
                lab_by_id[item["labId"]]["capacity"],
                by_id[item["courseId"]]["studentCount"],
                "排进了容量不足的实验室",
            )
            slot = (item["week"], item["day"], item["session"], item["labId"])
            self.assertNotIn(slot, lab_slots, "同一实验室同一节次被重复占用")
            lab_slots.add(slot)
            slot_courses[(item["week"], item["day"], item["session"])].add(item["courseId"])
            blocks[(item["courseId"], item["week"])].append(item)

        # 核心回归：同一学生在同一节次最多只能有一门课
        for slot, cids in slot_courses.items():
            for sid, enrolled in student_courses.items():
                clash = cids & enrolled
                self.assertLessEqual(len(clash), 1,
                                     f"学生 {sid} 在 {slot} 被安排了多门课：{clash}")

        for (cid, week), items in blocks.items():
            spw = by_id[cid]["sessionsPerWeek"]
            self.assertEqual(len(items), spw, f"课程 {cid} 第 {week} 周节数不对")
            self.assertEqual(len({i["day"] for i in items}), 1, "同一周的课程块跨天了")
            self.assertEqual(len({i["labId"] for i in items}), 1, "同一周的课程块跨实验室了")
            sections = sorted(i["session"] for i in items)
            self.assertEqual(sections, list(range(sections[0], sections[0] + spw)),
                             "同一周的课程块节次不连续")
        return blocks, slot_courses

    def test_demo_data_has_no_student_clash_and_spreads_over_days(self):
        """与 DataInitializer 一致的演示数据：全员选全部课程。"""
        students = [1, 2]
        courses = [
            course(1, "数据结构", 8, 2, students),
            course(2, "计算机网络", 8, 2, students),
            course(3, "操作系统", 8, 2, students),
            course(4, "机器学习", 6, 2, students),
        ]
        result = schedule_courses(courses, make_labs(), time_limit_seconds=10)
        blocks, _ = self.assert_valid(result, courses, make_labs())

        days_by_week = defaultdict(dict)
        for (cid, week), items in blocks.items():
            days_by_week[week][cid] = items[0]["day"]
        for week, per_course in days_by_week.items():
            self.assertEqual(len(set(per_course.values())), len(per_course),
                             f"第 {week} 周共享学生的课程被排在了同一天")

    def test_independent_student_groups_can_run_in_parallel(self):
        """学生群体互不重叠时，不应被强行拆到不同天，仍可同一时段并行。"""
        courses = [
            course(1, "A1", 1, 2, [1]),
            course(2, "A2", 1, 2, [1]),
            course(3, "B1", 1, 2, [2]),
            course(4, "B2", 1, 2, [2]),
        ]
        lab_list = make_labs()
        result = schedule_courses(courses, lab_list, time_limit_seconds=10)
        _blocks, slot_courses = self.assert_valid(result, courses, lab_list)
        self.assertTrue(
            any(len(cids) > 1 for cids in slot_courses.values()),
            "两组互不重叠的课程应能在同一时段、不同实验室并行",
        )

    def test_courses_without_students_are_allowed(self):
        """无人选课的课程：studentIds 为空列表是合法输入。"""
        courses = [course(1, "公开课", 2, 2, [])]
        lab_list = make_labs(1)
        result = schedule_courses(courses, lab_list, time_limit_seconds=10)
        self.assert_valid(result, courses, lab_list)

    def test_capacity_violation_is_rejected(self):
        courses = [course(1, "大班", 1, 2, [1], count=31)]
        with self.assertRaisesRegex(ValueError, "没有实验室容量足够"):
            schedule_courses(courses, make_labs(capacity=30), time_limit_seconds=10)

    def test_missing_student_ids_is_rejected(self):
        bad = course(1, "缺字段", 1, 2, [1])
        del bad["studentIds"]
        with self.assertRaisesRegex(ValueError, "studentIds"):
            schedule_courses([bad], make_labs(), time_limit_seconds=10)

    def test_invalid_weeks_and_sessions_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "周数"):
            schedule_courses([course(1, "A", 17, 2, [1])], make_labs(), time_limit_seconds=10)
        with self.assertRaisesRegex(ValueError, "超出每天可排"):
            schedule_courses([course(1, "A", 1, SECTIONS + 1, [1])], make_labs(), time_limit_seconds=10)

    def test_empty_inputs_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "课程数据为空"):
            schedule_courses([], make_labs(), time_limit_seconds=10)
        with self.assertRaisesRegex(ValueError, "实验室数据为空"):
            schedule_courses([course(1, "A", 1, 2, [1])], [], time_limit_seconds=10)

    def test_diagnosis_mentions_student_load(self):
        message = _diagnose([course(1, "A", 2, 4, [1, 2])], make_labs())
        self.assertIn("单个学生每周最多需上", message)
        self.assertIn("全部实验室一周可提供", message)


class ApiContractTest(unittest.TestCase):
    """FastAPI 入参模型：studentIds 必填，避免静默跳过学生互斥。"""

    def test_student_ids_required(self):
        from pydantic import ValidationError

        payload = {
            "courseId": 1,
            "courseName": "A",
            "studentCount": 2,
            "weeks": 1,
            "sessionsPerWeek": 2,
        }
        with self.assertRaises(ValidationError):
            CourseDTO(**payload)
        CourseDTO(**payload, studentIds=[1, 2])


if __name__ == "__main__":
    unittest.main()
