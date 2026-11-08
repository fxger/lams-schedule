package org.example.lamspojo.vo;

import lombok.Data;

@Data
public class ScheduleVO {

    private Long id;
    private Integer week;
    private Integer day;
    private Integer section;
    private Long courseId;
    private String courseName;
    private Long labId;
    private String labName;
}
