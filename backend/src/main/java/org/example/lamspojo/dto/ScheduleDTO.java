package org.example.lamspojo.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 单条排课新增/修改入参（管理员手工调整用）
 */
@Data
public class ScheduleDTO {

    @NotNull(message = "周次不能为空")
    @Min(value = 1, message = "周次必须在 1-16 之间")
    @Max(value = 16, message = "周次必须在 1-16 之间")
    private Integer week;

    @NotNull(message = "星期不能为空")
    @Min(value = 1, message = "星期必须在 1-7 之间")
    @Max(value = 7, message = "星期必须在 1-7 之间")
    private Integer day;

    @NotNull(message = "节次不能为空")
    @Min(value = 1, message = "节次必须在 1-11 之间")
    @Max(value = 11, message = "节次必须在 1-11 之间")
    private Integer section;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private Long courseId;

    @NotNull(message = "实验室不能为空")
    private Long labId;

    private String labName;
}
