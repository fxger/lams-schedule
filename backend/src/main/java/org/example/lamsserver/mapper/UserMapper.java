package org.example.lamsserver.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.example.lamspojo.po.UserLoginPO;
import org.example.lamspojo.vo.StudentVO;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select id, username, password, real_name, user_type, status, created_time " +
            "from users where username = #{username}")
    UserLoginPO getByUsername(String username);

    @Select("select id, username, real_name from users where user_type = 'student' and status = 1 order by id")
    List<StudentVO> listStudents();

    @Select("select count(*) from users")
    int countAll();

    @Insert("insert into users (username, password, real_name, user_type) " +
            "values (#{username}, #{password}, #{realName}, #{userType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserLoginPO user);
}
