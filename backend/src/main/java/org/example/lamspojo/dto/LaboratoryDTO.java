package org.example.lamspojo.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 实验室新增/修改入参
 */
@Data
public class LaboratoryDTO {

    private Long id;

    @NotBlank(message = "实验室名称不能为空")
    private String name;

    private String description;

    private String location;

    @NotNull(message = "房间号不能为空")
    @Min(value = 1, message = "房间号必须为正整数")
    private Integer roomNumber;

    @NotNull(message = "容纳人数不能为空")
    @Min(value = 1, message = "容纳人数至少为 1")
    private Integer capacity;
}
