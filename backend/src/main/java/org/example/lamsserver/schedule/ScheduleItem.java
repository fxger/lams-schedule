package org.example.lamsserver.schedule;

import lombok.Data;

/**
 * Python 求解服务返回的单条排课结果
 */
@Data
public class ScheduleItem {

    private Long courseId;
    private String courseName;
    private Integer week;
    private Integer day;
    /** 起始节次，连续 periods 节 */
    private Integer session;
    private Long labId;
    private String labName;
}
