package org.example.lamspojo.vo;

import lombok.Data;

@Data
public class UserLoginVO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String token;
}
