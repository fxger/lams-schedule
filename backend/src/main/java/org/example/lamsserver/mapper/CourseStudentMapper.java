package org.example.lamsserver.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseStudentMapper {

    @Select("select student_id from course_students where course_id = #{courseId} order by student_id")
    List<Long> listStudentIds(Long courseId);

    @Delete("delete from course_students where course_id = #{courseId}")
    int deleteByCourseId(Long courseId);

    @Insert("insert into course_students (course_id, student_id) values (#{courseId}, #{studentId})")
    int insert(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
}
