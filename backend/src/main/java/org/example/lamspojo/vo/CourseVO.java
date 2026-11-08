package org.example.lamspojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class CourseVO {

    private Long id;
    private String name;
    private String code;
    private String semester;
    private Integer credits;
    private Integer weeks;
    private Integer periods;
    private String description;
    private Integer studentCount;
    private List<Long> studentIds;
}
