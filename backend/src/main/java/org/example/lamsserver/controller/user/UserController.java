package org.example.lamsserver.controller.user;

import org.example.lamscommon.result.Result;
import org.example.lamspojo.dto.UserLoginDTO;
import org.example.lamspojo.vo.StudentVO;
import org.example.lamspojo.vo.UserLoginVO;
import org.example.lamsserver.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 登录：管理员 / 学生 */
    @PostMapping("/auth/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /** 学生列表，供管理员给课程分配学生 */
    @GetMapping("/users/students")
    public Result<List<StudentVO>> listStudents() {
        return Result.success(userService.listStudents());
    }
}
