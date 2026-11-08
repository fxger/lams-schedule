package org.example.lamspojo.vo;

import lombok.Data;

@Data
public class LaboratoryVO {

    private Long id;
    private String name;
    private String description;
    private String location;
    private Integer roomNumber;
    private Integer capacity;
    private Integer status;
}
