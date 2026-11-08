package org.example.lamspojo.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CoursePO {

    private Long id;
    private String name;
    private String code;
    private String semester;
    private Integer credits;
    private Integer weeks;
    private Integer periods;
    private String description;
    private Integer status;
    private Integer studentCount;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
