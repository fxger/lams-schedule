package org.example.lamspojo.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 课程新增/修改入参
 */
@Data
public class CourseDTO {

    private Long id;

    @NotBlank(message = "课程名称不能为空")
    private String name;

    private String code;

    private String semester;

    @Min(value = 1, message = "学分至少为 1")
    private Integer credits;

    /** 需要安排到第几周（1..weeks） */
    @NotNull(message = "周数不能为空")
    @Min(value = 1, message = "周数至少为 1")
    private Integer weeks;

    /** 每周连续安排的节数 */
    @NotNull(message = "每周节数不能为空")
    @Min(value = 1, message = "每周节数至少为 1")
    private Integer periods;

    private String description;

    /** 选课学生 id 列表，保存时整体覆盖 */
    private List<Long> studentIds;
}
