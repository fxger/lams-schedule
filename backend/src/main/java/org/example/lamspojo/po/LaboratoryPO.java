package org.example.lamspojo.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LaboratoryPO {

    private Long id;
    private String name;
    private String description;
    private String location;
    private Integer roomNumber;
    private Integer capacity;
    private Integer status;
    private LocalDateTime createdTime;
}
