package org.example.lamsserver.service;

import org.example.lamspojo.dto.UserLoginDTO;
import org.example.lamspojo.vo.StudentVO;
import org.example.lamspojo.vo.UserLoginVO;

import java.util.List;

public interface UserService {

    UserLoginVO login(UserLoginDTO userLoginDTO);

    List<StudentVO> listStudents();
}
