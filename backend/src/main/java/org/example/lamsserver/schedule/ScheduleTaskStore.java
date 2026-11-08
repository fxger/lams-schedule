package org.example.lamsserver.schedule;

import org.example.lamspojo.vo.ScheduleTaskVO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 排课任务状态的内存存储（任务为短生命周期，重启后丢失可接受）
 */
@Component
public class ScheduleTaskStore {

    private static final int MAX_TASKS = 50;

    private final Map<String, ScheduleTaskVO> tasks = new ConcurrentHashMap<>();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final AtomicLong sequence = new AtomicLong();

    /** 抢占执行权，同一时刻只允许一个自动排课任务 */
    public boolean tryAcquire() {
        return busy.compareAndSet(false, true);
    }

    public void release() {
        busy.set(false);
    }

    public void running(String taskId, int progress, String message) {
        put(taskId, ScheduleTaskVO.of("RUNNING", progress, message));
    }

    public void completed(String taskId, int total, String message) {
        ScheduleTaskVO vo = ScheduleTaskVO.of("COMPLETED", 100, message);
        vo.setTotal(total);
        vo.setErrorCount(0);
        put(taskId, vo);
    }

    public void failed(String taskId, String message) {
        ScheduleTaskVO vo = ScheduleTaskVO.of("FAILED", 100, message);
        put(taskId, vo);
    }

    public ScheduleTaskVO get(String taskId) {
        ScheduleTaskVO vo = tasks.get(taskId);
        return vo == null ? ScheduleTaskVO.of("NOT_FOUND", 0, "任务不存在") : vo;
    }

    /** 是否已有排队/执行中的任务 */
    public boolean hasRunning() {
        return tasks.values().stream().anyMatch(vo -> "RUNNING".equals(vo.getStatus()));
    }

    private void put(String taskId, ScheduleTaskVO vo) {
        // 用单调序号而非时间戳：同一毫秒内连续创建任务时淘汰顺序依然确定
        vo.setOrderSeq(sequence.incrementAndGet());
        // 只在"新增任务且已满"时淘汰最早写入的一条；更新已有任务不应挤掉别人
        if (!tasks.containsKey(taskId) && tasks.size() >= MAX_TASKS) {
            tasks.entrySet().stream()
                    .min(Comparator.comparingLong(e -> {
                        Long orderSeq = e.getValue().getOrderSeq();
                        return orderSeq == null ? 0L : orderSeq;
                    }))
                    .map(Map.Entry::getKey)
                    .ifPresent(tasks::remove);
        }
        tasks.put(taskId, vo);
    }
}
