package org.example.lamsserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.lamscommon.properties.JwtProperties;
import org.example.lamscommon.utils.JwtUtil;
import org.example.lamspojo.dto.UserLoginDTO;
import org.example.lamspojo.po.UserLoginPO;
import org.example.lamspojo.vo.StudentVO;
import org.example.lamspojo.vo.UserLoginVO;
import org.example.lamsserver.exception.BizException;
import org.example.lamsserver.mapper.UserMapper;
import org.example.lamsserver.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper, JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        UserLoginPO user = userMapper.getByUsername(userLoginDTO.getUsername());
        if (user == null || !passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        if (!user.getUserType().equalsIgnoreCase(userLoginDTO.getRole())) {
            throw new BizException("该账号不是" + ("admin".equalsIgnoreCase(userLoginDTO.getRole()) ? "管理员" : "学生")
                    + "角色，请重新选择登录身份");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getUserType());
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);

        UserLoginVO vo = new UserLoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getUserType());
        vo.setToken(token);
        log.info("用户 {} 登录成功，角色 {}", user.getUsername(), user.getUserType());
        return vo;
    }

    @Override
    public List<StudentVO> listStudents() {
        return userMapper.listStudents();
    }
}
