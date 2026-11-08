package org.example.lamspojo.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SchedulePO {

    private Long id;
    private Integer week;
    private Integer day;
    private Integer section;
    private Long courseId;
    private String courseName;
    private Long labId;
    private String labName;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
