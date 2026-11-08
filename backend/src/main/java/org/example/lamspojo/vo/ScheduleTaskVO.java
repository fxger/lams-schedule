package org.example.lamspojo.vo;

import lombok.Data;

/**
 * 自动排课任务状态（前端每 3 秒轮询 /schedule/task-status/{taskId}）
 */
@Data
public class ScheduleTaskVO {

    /** STARTING / RUNNING / COMPLETED / FAILED / NOT_FOUND */
    private String status;
    private Integer progress;
    private String message;
    private Integer total;
    private Integer errorCount;
    /** 单调递增的写入序号，由 ScheduleTaskStore 分配，用于淘汰最旧任务 */
    private Long orderSeq;

    public static ScheduleTaskVO of(String status, int progress, String message) {
        ScheduleTaskVO vo = new ScheduleTaskVO();
        vo.status = status;
        vo.progress = progress;
        vo.message = message;
        return vo;
    }
}
