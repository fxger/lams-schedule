package org.example.lamsserver.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.lamspojo.po.SchedulePO;

import java.util.List;

@Mapper
public interface ScheduleMapper {

    String BASE_COLUMNS = "id, week, day, section, course_id, course_name, lab_id, lab_name, " +
            "created_by, updated_by, created_time, updated_time ";

    @Select("select " + BASE_COLUMNS + "from schedules where week = #{week} order by day, section, lab_id")
    List<SchedulePO> listByWeek(Integer week);

    @Select("select " + BASE_COLUMNS + "from schedules order by week, day, section")
    List<SchedulePO> listAll();

    /** 学生本人选课对应的课表 */
    @Select("select s.id, s.week, s.day, s.section, s.course_id, s.course_name, s.lab_id, s.lab_name, " +
            "s.created_by, s.updated_by, s.created_time, s.updated_time " +
            "from schedules s join course_students cs on cs.course_id = s.course_id " +
            "where cs.student_id = #{studentId} and s.week = #{week} " +
            "order by s.day, s.section")
    List<SchedulePO> listByStudent(@Param("studentId") Long studentId, @Param("week") Integer week);

    @Select("select " + BASE_COLUMNS + "from schedules where id = #{id}")
    SchedulePO getById(Long id);

    /** 同一实验室同一时间是否已被占用（新增时使用） */
    @Select("select count(*) from schedules where week = #{week} and day = #{day} and section = #{section} " +
            "and lab_id = #{labId}")
    int countConflict(@Param("week") Integer week, @Param("day") Integer day,
                      @Param("section") Integer section, @Param("labId") Long labId);

    /** 同一实验室同一时间是否已被其它记录占用（修改时使用） */
    @Select("select count(*) from schedules where week = #{week} and day = #{day} and section = #{section} " +
            "and lab_id = #{labId} and id <> #{excludeId}")
    int countConflictExcluding(@Param("week") Integer week, @Param("day") Integer day,
                               @Param("section") Integer section, @Param("labId") Long labId,
                               @Param("excludeId") Long excludeId);

    @Insert("insert into schedules (week, day, section, course_id, course_name, lab_id, lab_name, " +
            "created_by, updated_by) values (#{week}, #{day}, #{section}, #{courseId}, #{courseName}, " +
            "#{labId}, #{labName}, #{createdBy}, #{updatedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SchedulePO schedule);

    @Update("update schedules set week = #{week}, day = #{day}, section = #{section}, course_id = #{courseId}, " +
            "course_name = #{courseName}, lab_id = #{labId}, lab_name = #{labName}, " +
            "updated_by = #{updatedBy}, updated_time = current_timestamp where id = #{id}")
    int update(SchedulePO schedule);

    @Delete("delete from schedules where id = #{id}")
    int deleteById(Long id);

    @Delete("delete from schedules")
    int deleteAll();

    @Delete("delete from schedules where course_id = #{courseId}")
    int deleteByCourseId(Long courseId);

    @Select("select count(*) from schedules where lab_id = #{labId}")
    int countByLabId(Long labId);
}
