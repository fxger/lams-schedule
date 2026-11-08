package org.example.lamspojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** admin / student，登录时必须与账号角色一致 */
    @NotBlank(message = "角色不能为空")
    private String role;
}
