package org.example.lamsserver.schedule;

import lombok.extern.slf4j.Slf4j;
import org.example.lamspojo.po.CoursePO;
import org.example.lamspojo.po.LaboratoryPO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 异步执行自动排课：调用求解服务 -> 校验 -> 落库 -> 更新任务状态
 */
@Service
@Slf4j
public class ScheduleTaskExecutor {

    private final PythonAlgoClient pythonAlgoClient;
    private final ScheduleWriter scheduleWriter;
    private final ScheduleTaskStore taskStore;

    public ScheduleTaskExecutor(PythonAlgoClient pythonAlgoClient,
                                ScheduleWriter scheduleWriter,
                                ScheduleTaskStore taskStore) {
        this.pythonAlgoClient = pythonAlgoClient;
        this.scheduleWriter = scheduleWriter;
        this.taskStore = taskStore;
    }

    @Async("scheduleExecutor")
    public void execute(String taskId, List<CoursePO> courses, List<LaboratoryPO> labs, String operator) {
        try {
            taskStore.running(taskId, 30, "正在调用 OR-Tools 求解服务...");
            List<ScheduleItem> items = pythonAlgoClient.solve(courses, labs);

            taskStore.running(taskId, 70, "求解完成，正在校验并写入数据库...");
            int count = scheduleWriter.replaceAll(items, scheduleWriter.indexCourses(courses),
                    scheduleWriter.indexLabs(labs), operator);

            taskStore.completed(taskId, count, "排课成功，共生成 " + count + " 条记录");
        } catch (Exception e) {
            log.error("自动排课任务 {} 执行失败", taskId, e);
            taskStore.failed(taskId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            // 无论成功、失败还是异常，都必须归还执行权，否则后续自动排课会被永久拒绝
            taskStore.release();
        }
    }
}
