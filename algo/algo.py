"""排课求解核心：OR-Tools CP-SAT。

输入：课程列表（weeks 周数、sessionsPerWeek 每周节数、studentCount 人数、studentIds 选课学生）
     实验室列表（capacity 容量）
输出：每个"节次"一条记录 {courseId, courseName, week, day, session, labId, labName}
"""

from collections import defaultdict

from ortools.sat.python import cp_model

DAYS = 7
SECTIONS = 11
MAX_WEEKS = 16
SOLVER_MAX_SECONDS = 60
SEARCH_WORKERS = 8


def schedule_courses(courses, labs, time_limit_seconds=SOLVER_MAX_SECONDS):
    """返回排课结果列表；无可行解或输入非法时抛出 ValueError（带诊断信息）。"""
    if not courses:
        raise ValueError("课程数据为空，无法排课")
    if not labs:
        raise ValueError("实验室数据为空，无法排课")

    course_by_id = _validate_input(courses, labs)
    model = cp_model.CpModel()

    # 变量 x[(course_id, week, day, start_section, lab_id)]：
    # 该课程在第 week 周第 day 天从 start_section 开始连续上课，占用某实验室
    x = {}
    for course in courses:
        cid = course["courseId"]
        spw = course["sessionsPerWeek"]
        fits = [lab for lab in labs if lab["capacity"] >= course["studentCount"]]
        for week in range(1, course["weeks"] + 1):
            for lab in fits:
                for day in range(1, DAYS + 1):
                    for start in range(1, SECTIONS - spw + 2):
                        x[(cid, week, day, start, lab["labId"])] = model.NewBoolVar(
                            f"x_{cid}_{week}_{day}_{start}_{lab['labId']}"
                        )

    # 按 (course, week) 索引变量：每门课每周恰好选一个连续块
    course_week_vars = defaultdict(list)
    for (cid, week, day, _start, _lab_id), var in x.items():
        course_week_vars[(cid, week)].append((day, var))
    for course in courses:
        for week in range(1, course["weeks"] + 1):
            entries = course_week_vars[(course["courseId"], week)]
            if not entries:
                raise ValueError(
                    f"课程《{course['courseName']}》没有任何容量足够的实验室可用"
                )
            model.AddExactlyOne([var for _day, var in entries])

    # 按 (week, day, section) 预索引"占用"，避免逐格遍历全部变量（原来约 O(W*D*S*L*|x|)）
    occupancy = defaultdict(list)
    for (cid, week, day, start, lab_id), var in x.items():
        for offset in range(course_by_id[cid]["sessionsPerWeek"]):
            occupancy[(week, day, start + offset)].append((cid, lab_id, var))

    # 约束 A：同一实验室同一节次最多一门课
    for entries in occupancy.values():
        by_lab = defaultdict(list)
        for _cid, lab_id, var in entries:
            by_lab[lab_id].append(var)
        for variables in by_lab.values():
            if len(variables) > 1:
                model.AddAtMostOne(variables)

    # 约束 B：同一学生同一节次只能上一门课（跨实验室也互斥）
    # 同时覆盖"同一门课同一节次不能占两个位置"，故不再单独建该约束
    student_courses = defaultdict(set)
    for course in courses:
        for student_id in course["studentIds"]:
            student_courses[student_id].add(course["courseId"])
    for entries in occupancy.values():
        for course_ids in student_courses.values():
            variables = [var for cid, _lab_id, var in entries if cid in course_ids]
            if len(variables) > 1:
                model.AddAtMostOne(variables)

    # 约束 C：同一周里「存在共同学生」的课程分散到不同星期，避免学生课表挤在一天。
    # 只在冲突组内部强制：学生群体互不重叠的课程仍可排在同一天的不同实验室。
    # 组内课程数超过 7 时不强制（一周只有 7 天），此时约束 B 已能保证不撞课。
    max_weeks = max(course["weeks"] for course in courses)
    conflict_groups = _conflict_groups(student_courses, [c["courseId"] for c in courses])
    for week in range(1, max_weeks + 1):
        day_vars = {}
        for course in courses:
            cid = course["courseId"]
            if week > course["weeks"]:
                continue
            entries = course_week_vars[(cid, week)]
            day_var = model.NewIntVar(1, DAYS, f"day_c{cid}_w{week}")
            # AddExactlyOne 保证这些变量里恰好一个为 1，因此加权和就是被选中的星期
            model.Add(day_var == sum(day * var for day, var in entries))
            day_vars[cid] = day_var
        for group in conflict_groups:
            variables = [day_vars[cid] for cid in group if cid in day_vars]
            if 1 < len(variables) <= DAYS:
                model.AddAllDifferent(variables)

    # 目标：在满足上述约束的前提下，尽量排在靠前的星期与节次，使课表紧凑可读
    model.Minimize(
        sum(var * ((day - 1) * SECTIONS + start) for (_c, _w, day, start, _l), var in x.items())
    )

    solver = cp_model.CpSolver()
    solver.parameters.max_time_in_seconds = time_limit_seconds
    solver.parameters.num_search_workers = SEARCH_WORKERS
    status = solver.Solve(model)

    if status not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
        if status == cp_model.INFEASIBLE:
            raise ValueError(_diagnose(courses, labs))
        raise ValueError(f"求解未得到可行解（状态 {solver.StatusName(status)}），请调整数据后重试")

    lab_names = {lab["labId"]: lab["labName"] for lab in labs}
    results = []
    for (cid, week, day, start, lab_id), var in x.items():
        if solver.Value(var) != 1:
            continue
        course = course_by_id[cid]
        for offset in range(course["sessionsPerWeek"]):
            results.append(
                {
                    "courseId": cid,
                    "courseName": course["courseName"],
                    "week": week,
                    "day": day,
                    "session": start + offset,
                    "labId": lab_id,
                    "labName": lab_names[lab_id],
                }
            )

    results.sort(key=lambda item: (item["week"], item["day"], item["session"], item["labId"]))
    return results


def _conflict_groups(student_courses, course_ids):
    """并查集：共享至少一名学生的课程归为同一冲突组，返回课程 ID 分组。"""
    parent = {cid: cid for cid in course_ids}

    def find(cid):
        while parent[cid] != cid:
            parent[cid] = parent[parent[cid]]
            cid = parent[cid]
        return cid

    for cids in student_courses.values():
        members = [cid for cid in cids if cid in parent]
        if not members:
            continue
        root = find(members[0])
        for cid in members[1:]:
            parent[find(cid)] = root

    groups = defaultdict(list)
    for cid in course_ids:
        groups[find(cid)].append(cid)
    return list(groups.values())


def _validate_input(courses, labs):
    """提前拦截明显非法的输入，并返回 courseId -> course 索引。"""
    course_by_id = {}
    for course in courses:
        name = course.get("courseName", course.get("courseId", "?"))
        cid = course.get("courseId")
        if cid is None or cid in course_by_id:
            raise ValueError(f"课程 ID 缺失或重复：{cid}")
        course_by_id[cid] = course

        weeks = course.get("weeks", 0)
        if not 1 <= weeks <= MAX_WEEKS:
            raise ValueError(f"课程《{name}》周数 {weeks} 非法，应为 1-{MAX_WEEKS}")
        spw = course.get("sessionsPerWeek", 0)
        if not 1 <= spw <= SECTIONS:
            raise ValueError(f"课程《{name}》每周 {spw} 节，超出每天可排的 1-{SECTIONS} 节")
        if course.get("studentCount", 0) < 0:
            raise ValueError(f"课程《{name}》选课人数为负数")
        if not isinstance(course.get("studentIds"), list):
            raise ValueError(f"课程《{name}》缺少选课学生列表 studentIds，无法保证不撞课")

        fits = [lab for lab in labs if lab["capacity"] >= course["studentCount"]]
        if not fits:
            raise ValueError(
                f"课程《{name}》需要 {course['studentCount']} 个座位，"
                f"但没有实验室容量足够（当前最大容量 {max(lab['capacity'] for lab in labs)}）"
            )

    lab_ids = [lab["labId"] for lab in labs]
    if len(lab_ids) != len(set(lab_ids)):
        raise ValueError("实验室 ID 存在重复")
    return course_by_id


def _diagnose(courses, labs):
    """无解时给出人能看懂的诊断。"""
    message = []
    for course in courses:
        if not any(lab["capacity"] >= course["studentCount"] for lab in labs):
            message.append(f"课程《{course['courseName']}》无容量足够的实验室")
    if message:
        return "无法排课：" + "；".join(message)

    student_load = defaultdict(int)
    for course in courses:
        for student_id in course["studentIds"]:
            student_load[student_id] += course["sessionsPerWeek"]
    max_week = max(course["weeks"] for course in courses)
    total_capacity = len(labs) * DAYS * SECTIONS
    max_demand = max(
        sum(c["sessionsPerWeek"] for c in courses if c["weeks"] >= week)
        for week in range(1, max_week + 1)
    )
    max_student_load = max(student_load.values(), default=0)
    return (
        "无法排课：约束冲突，不存在可行解。"
        f"单周最多需排 {max_demand} 节，全部实验室一周可提供 {total_capacity} 节；"
        f"单个学生每周最多需上 {max_student_load} 节，一周共 {DAYS * SECTIONS} 个可用节次。"
        "请增加实验室、减少每周节数、调整周数或选课关系。"
    )
