package org.example.lamsserver.schedule;

import lombok.extern.slf4j.Slf4j;
import org.example.lamspojo.po.CoursePO;
import org.example.lamspojo.po.LaboratoryPO;
import org.example.lamsserver.exception.BizException;
import org.example.lamsserver.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 求解结果的校验与落库：先清空旧排课，再逐条校验后写入；任一条非法则整体回滚。
 */
@Service
@Slf4j
public class ScheduleWriter {

    private final ScheduleMapper scheduleMapper;

    public ScheduleWriter(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public int replaceAll(List<ScheduleItem> items, Map<Long, CoursePO> courseMap,
                          Map<Long, LaboratoryPO> labMap, String operator) {
        List<ScheduleItem> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> usedSlots = new HashSet<>();

        for (ScheduleItem item : items) {
            String error = validate(item, courseMap, labMap, usedSlots);
            if (error != null) {
                errors.add(error);
            } else {
                valid.add(item);
            }
        }
        if (!errors.isEmpty()) {
            throw new BizException("求解结果校验失败，" + errors.size() + " 条非法，已回滚。示例：" +
                    String.join("；", errors.subList(0, Math.min(5, errors.size()))));
        }

        // 完整性校验：每门课应恰好排满 weeks * periods 个节次
        Map<Long, Integer> actualCount = new LinkedHashMap<>();
        for (ScheduleItem item : valid) {
            actualCount.merge(item.getCourseId(), 1, Integer::sum);
        }
        for (CoursePO course : courseMap.values()) {
            int expected = (course.getWeeks() == null ? 8 : course.getWeeks())
                    * (course.getPeriods() == null ? 2 : course.getPeriods());
            int actual = actualCount.getOrDefault(course.getId(), 0);
            if (actual != expected) {
                throw new BizException("课程【" + course.getName() + "】排课不完整：期望 " + expected
                        + " 节，实际 " + actual + " 节，已回滚");
            }
        }

        scheduleMapper.deleteAll();
        for (ScheduleItem item : valid) {
            org.example.lamspojo.po.SchedulePO po = new org.example.lamspojo.po.SchedulePO();
            po.setWeek(item.getWeek());
            po.setDay(item.getDay());
            po.setSection(item.getSession());
            po.setCourseId(item.getCourseId());
            po.setCourseName(item.getCourseName());
            po.setLabId(item.getLabId());
            po.setLabName(item.getLabName());
            po.setCreatedBy(operator);
            po.setUpdatedBy(operator);
            scheduleMapper.insert(po);
        }
        log.info("排课结果已写入数据库，共 {} 条", valid.size());
        return valid.size();
    }

    private String validate(ScheduleItem item, Map<Long, CoursePO> courseMap,
                            Map<Long, LaboratoryPO> labMap, Set<String> usedSlots) {
        if (item.getWeek() == null || item.getWeek() < 1 || item.getWeek() > 16) {
            return "周次非法:" + item.getWeek();
        }
        if (item.getDay() == null || item.getDay() < 1 || item.getDay() > 7) {
            return "星期非法:" + item.getDay();
        }
        if (item.getSession() == null || item.getSession() < 1 || item.getSession() > 11) {
            return "节次非法:" + item.getSession();
        }
        if (item.getLabId() == null || !labMap.containsKey(item.getLabId())) {
            return "实验室不存在:" + item.getLabId();
        }
        if (item.getCourseId() == null || !courseMap.containsKey(item.getCourseId())) {
            return "课程不存在:" + item.getCourseId();
        }
        String slot = item.getWeek() + "-" + item.getDay() + "-" + item.getSession() + "-" + item.getLabId();
        if (!usedSlots.add(slot)) {
            return "同一实验室时间冲突:" + slot;
        }
        return null;
    }

    /** 供调试/测试使用 */
    public Map<Long, CoursePO> indexCourses(List<CoursePO> courses) {
        Map<Long, CoursePO> map = new LinkedHashMap<>();
        courses.forEach(c -> map.put(c.getId(), c));
        return map;
    }

    public Map<Long, LaboratoryPO> indexLabs(List<LaboratoryPO> labs) {
        Map<Long, LaboratoryPO> map = new LinkedHashMap<>();
        labs.forEach(l -> map.put(l.getId(), l));
        return map;
    }
}
