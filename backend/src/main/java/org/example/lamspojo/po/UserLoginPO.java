package org.example.lamspojo.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLoginPO {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String userType;
    private Integer status;
    private LocalDateTime createdTime;
}
