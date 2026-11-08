package org.example.lamsserver.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.lamspojo.po.CoursePO;

import java.util.List;

@Mapper
public interface CourseMapper {

    String BASE_COLUMNS = "c.id, c.name, c.code, c.semester, c.credits, c.weeks, c.periods, " +
            "c.description, c.status, c.created_time, c.updated_time, " +
            "(select count(*) from course_students cs where cs.course_id = c.id) as student_count ";

    @Select("select " + BASE_COLUMNS + "from courses c where c.status = 1 order by c.id")
    List<CoursePO> list();

    @Select("select " + BASE_COLUMNS + "from courses c where c.id = #{id}")
    CoursePO getById(Long id);

    @Insert("insert into courses (name, code, semester, credits, weeks, periods, description, status) " +
            "values (#{name}, #{code}, #{semester}, #{credits}, #{weeks}, #{periods}, #{description}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CoursePO course);

    @Update("update courses set name = #{name}, code = #{code}, semester = #{semester}, credits = #{credits}, " +
            "weeks = #{weeks}, periods = #{periods}, description = #{description}, updated_time = current_timestamp " +
            "where id = #{id}")
    int update(CoursePO course);

    @Update("update courses set status = 0, updated_time = current_timestamp where id = #{id}")
    int deleteById(Long id);

    @Select("select count(*) from schedules where course_id = #{courseId}")
    int countSchedules(@Param("courseId") Long courseId);
}
