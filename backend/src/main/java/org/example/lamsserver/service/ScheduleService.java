package org.example.lamsserver.service;

import org.example.lamspojo.dto.ScheduleDTO;
import org.example.lamspojo.vo.ScheduleTaskVO;
import org.example.lamspojo.vo.ScheduleVO;

import java.util.List;

public interface ScheduleService {

    List<ScheduleVO> listByWeek(Integer week);

    List<ScheduleVO> listByStudent(Long studentId, Integer week);

    ScheduleVO insert(ScheduleDTO dto, String operator);

    ScheduleVO update(Long id, ScheduleDTO dto, String operator);

    void delete(Long id);

    void deleteAll();

    /** 启动异步自动排课，返回 taskId */
    String autoSchedule(String operator);

    ScheduleTaskVO getTaskStatus(String taskId);
}
