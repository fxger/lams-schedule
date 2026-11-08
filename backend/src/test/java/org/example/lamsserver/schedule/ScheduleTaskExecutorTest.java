package org.example.lamsserver.schedule;

import org.example.lamspojo.po.CoursePO;
import org.example.lamspojo.po.LaboratoryPO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 自动排课任务的回归测试。
 *
 * 重点：execute() 无论成功还是失败都必须归还 ScheduleTaskStore 的执行权，
 * 否则第一次排课之后所有后续排课都会被「已有排课任务正在执行」永久拒绝。
 */
class ScheduleTaskExecutorTest {

    private final List<CoursePO> courses = Collections.emptyList();
    private final List<LaboratoryPO> labs = Collections.emptyList();

    private PythonAlgoClient clientReturning(List<ScheduleItem> items) {
        PythonAlgoClient client = mock(PythonAlgoClient.class);
        when(client.solve(anyList(), anyList())).thenReturn(items);
        return client;
    }

    private ScheduleWriter writerReturning(int count) {
        ScheduleWriter writer = mock(ScheduleWriter.class);
        when(writer.replaceAll(any(), any(), any(), anyString())).thenReturn(count);
        return writer;
    }

    @Test
    void releasesExecutionPermitAfterSuccess() {
        ScheduleTaskStore store = new ScheduleTaskStore();
        assertTrue(store.tryAcquire());
        ScheduleTaskExecutor executor = new ScheduleTaskExecutor(
                clientReturning(Collections.emptyList()), writerReturning(0), store);

        // 直接调用实例方法（不经 Spring 代理），@Async 不生效，便于同步断言
        executor.execute("task-success", courses, labs, "admin");

        assertEquals("COMPLETED", store.get("task-success").getStatus());
        assertTrue(store.tryAcquire(), "成功结束后必须归还执行权，否则无法再次自动排课");
    }

    @Test
    void releasesExecutionPermitAfterFailure() {
        ScheduleTaskStore store = new ScheduleTaskStore();
        assertTrue(store.tryAcquire());
        PythonAlgoClient client = mock(PythonAlgoClient.class);
        when(client.solve(anyList(), anyList()))
                .thenThrow(new IllegalStateException("算法服务不可用"));
        ScheduleTaskExecutor executor = new ScheduleTaskExecutor(client, writerReturning(0), store);

        executor.execute("task-failure", courses, labs, "admin");

        assertEquals("FAILED", store.get("task-failure").getStatus());
        assertTrue(store.tryAcquire(), "求解失败后也必须归还执行权，否则失败一次就再也不能排课");
    }

    @Test
    void evictsOldestTaskWhenFull() {
        ScheduleTaskStore store = new ScheduleTaskStore();
        for (int i = 0; i < 50; i++) {
            store.running("task-" + i, 10, "running");
        }
        assertEquals("RUNNING", store.get("task-0").getStatus());

        store.running("task-50", 10, "running");

        assertEquals("NOT_FOUND", store.get("task-0").getStatus(), "最旧任务应被淘汰");
        assertEquals("RUNNING", store.get("task-49").getStatus());
        assertEquals("RUNNING", store.get("task-50").getStatus());
    }

    @Test
    void updatingExistingTaskDoesNotEvictOthers() {
        ScheduleTaskStore store = new ScheduleTaskStore();
        for (int i = 0; i < 50; i++) {
            store.running("task-" + i, 10, "running");
        }

        store.running("task-25", 50, "进度更新");

        assertEquals("RUNNING", store.get("task-0").getStatus(), "更新已有任务不应挤掉别人");
        assertEquals(50, store.get("task-25").getProgress());
    }
}
