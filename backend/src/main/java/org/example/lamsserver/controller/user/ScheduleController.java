package org.example.lamsserver.controller.user;

import org.example.lamscommon.context.BaseContext;
import org.example.lamscommon.result.Result;
import org.example.lamspojo.dto.ScheduleDTO;
import org.example.lamspojo.vo.ScheduleTaskVO;
import org.example.lamspojo.vo.ScheduleVO;
import org.example.lamsserver.service.ScheduleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** 按周查看全量课表（管理员） */
    @GetMapping
    public Result<List<ScheduleVO>> listByWeek(@RequestParam Integer week) {
        return Result.success(scheduleService.listByWeek(week));
    }

    /** 学生查看自己的课表 */
    @GetMapping("/mine")
    public Result<List<ScheduleVO>> mine(@RequestParam(required = false) Integer week) {
        return Result.success(scheduleService.listByStudent(BaseContext.getCurrentId(), week));
    }

    @PostMapping
    public Result<ScheduleVO> insert(@Valid @RequestBody ScheduleDTO dto) {
        return Result.success(scheduleService.insert(dto, BaseContext.getCurrentUsername()));
    }

    @PutMapping("/{id}")
    public Result<ScheduleVO> update(@PathVariable Long id, @Valid @RequestBody ScheduleDTO dto) {
        return Result.success(scheduleService.update(id, dto, BaseContext.getCurrentUsername()));
    }

    @DeleteMapping("/all")
    public Result<Void> deleteAll() {
        scheduleService.deleteAll();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.success();
    }

    /** 触发自动排课，返回 taskId */
    @PostMapping("/auto")
    public Result<String> autoSchedule() {
        return Result.success(scheduleService.autoSchedule(BaseContext.getCurrentUsername()));
    }

    /** 轮询任务状态 */
    @GetMapping("/task-status/{taskId}")
    public Result<ScheduleTaskVO> taskStatus(@PathVariable String taskId) {
        return Result.success(scheduleService.getTaskStatus(taskId));
    }
}
