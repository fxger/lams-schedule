package org.example.lamsserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.lamspojo.dto.ScheduleDTO;
import org.example.lamspojo.po.CoursePO;
import org.example.lamspojo.po.LaboratoryPO;
import org.example.lamspojo.po.SchedulePO;
import org.example.lamspojo.vo.ScheduleTaskVO;
import org.example.lamspojo.vo.ScheduleVO;
import org.example.lamsserver.exception.BizException;
import org.example.lamsserver.mapper.CourseMapper;
import org.example.lamsserver.mapper.LaboratoryMapper;
import org.example.lamsserver.mapper.ScheduleMapper;
import org.example.lamsserver.schedule.ScheduleTaskExecutor;
import org.example.lamsserver.schedule.ScheduleTaskStore;
import org.example.lamsserver.service.ScheduleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final LaboratoryMapper laboratoryMapper;
    private final CourseMapper courseMapper;
    private final ScheduleTaskExecutor taskExecutor;
    private final ScheduleTaskStore taskStore;

    public ScheduleServiceImpl(ScheduleMapper scheduleMapper,
                               LaboratoryMapper laboratoryMapper,
                               CourseMapper courseMapper,
                               ScheduleTaskExecutor taskExecutor,
                               ScheduleTaskStore taskStore) {
        this.scheduleMapper = scheduleMapper;
        this.laboratoryMapper = laboratoryMapper;
        this.courseMapper = courseMapper;
        this.taskExecutor = taskExecutor;
        this.taskStore = taskStore;
    }

    @Override
    public List<ScheduleVO> listByWeek(Integer week) {
        if (week == null || week < 1 || week > 16) {
            throw new BizException("周次必须在 1-16 之间");
        }
        return scheduleMapper.listByWeek(week).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> listByStudent(Long studentId, Integer week) {
        if (studentId == null) {
            throw new BizException("未获取到当前学生身份");
        }
        int queryWeek = (week == null || week < 1 || week > 16) ? 1 : week;
        return scheduleMapper.listByStudent(studentId, queryWeek).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ScheduleVO insert(ScheduleDTO dto, String operator) {
        validate(dto, null);
        SchedulePO po = fromDTO(dto);
        po.setCreatedBy(operator);
        po.setUpdatedBy(operator);
        scheduleMapper.insert(po);
        return toVO(scheduleMapper.getById(po.getId()));
    }

    @Override
    public ScheduleVO update(Long id, ScheduleDTO dto, String operator) {
        SchedulePO existing = scheduleMapper.getById(id);
        if (existing == null) {
            throw new BizException("排课记录不存在");
        }
        validate(dto, id);
        SchedulePO po = fromDTO(dto);
        po.setId(id);
        po.setUpdatedBy(operator);
        scheduleMapper.update(po);
        return toVO(scheduleMapper.getById(id));
    }

    @Override
    public void delete(Long id) {
        if (scheduleMapper.getById(id) == null) {
            throw new BizException("排课记录不存在");
        }
        scheduleMapper.deleteById(id);
    }

    @Override
    public void deleteAll() {
        int count = scheduleMapper.deleteAll();
        log.info("已清空所有排课记录，共 {} 条", count);
    }

    @Override
    public String autoSchedule(String operator) {
        List<CoursePO> courses = courseMapper.list();
        List<LaboratoryPO> labs = laboratoryMapper.list();
        if (courses.isEmpty()) {
            throw new BizException("课程数据为空，无法自动排课");
        }
        if (labs.isEmpty()) {
            throw new BizException("实验室数据为空，无法自动排课");
        }
        if (!taskStore.tryAcquire()) {
            throw new BizException("已有排课任务正在执行，请稍后再试");
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        log.info("创建自动排课任务 {}，课程 {} 门，实验室 {} 间，操作人 {}",
                taskId, courses.size(), labs.size(), operator);
        taskStore.running(taskId, 10, "任务已创建，正在准备数据...");

        try {
            // @Async 正常提交时由 ScheduleTaskExecutor 在 finally 中释放执行权；
            // 若线程池直接拒绝、execute 根本没运行，则由此处兜底释放，避免锁泄漏。
            taskExecutor.execute(taskId, courses, labs, operator);
        } catch (RuntimeException e) {
            taskStore.release();
            taskStore.failed(taskId, "排课任务提交失败：" + e.getMessage());
            throw e;
        }
        return taskId;
    }

    @Override
    public ScheduleTaskVO getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }

    private void validate(ScheduleDTO dto, Long excludeId) {
        if (laboratoryMapper.countById(dto.getLabId()) == 0) {
            throw new BizException("实验室不存在");
        }
        int conflict = excludeId == null
                ? scheduleMapper.countConflict(dto.getWeek(), dto.getDay(), dto.getSection(), dto.getLabId())
                : scheduleMapper.countConflictExcluding(dto.getWeek(), dto.getDay(), dto.getSection(),
                dto.getLabId(), excludeId);
        if (conflict > 0) {
            throw new BizException("该实验室在 第" + dto.getWeek() + "周 星期" + dto.getDay()
                    + " 第" + dto.getSection() + "节 已有排课");
        }
    }

    private SchedulePO fromDTO(ScheduleDTO dto) {
        SchedulePO po = new SchedulePO();
        po.setWeek(dto.getWeek());
        po.setDay(dto.getDay());
        po.setSection(dto.getSection());
        po.setCourseId(dto.getCourseId());
        po.setCourseName(dto.getCourseName());
        po.setLabId(dto.getLabId());
        po.setLabName(dto.getLabName());
        return po;
    }

    private ScheduleVO toVO(SchedulePO po) {
        ScheduleVO vo = new ScheduleVO();
        vo.setId(po.getId());
        vo.setWeek(po.getWeek());
        vo.setDay(po.getDay());
        vo.setSection(po.getSection());
        vo.setCourseId(po.getCourseId());
        vo.setCourseName(po.getCourseName());
        vo.setLabId(po.getLabId());
        vo.setLabName(po.getLabName());
        return vo;
    }
}
