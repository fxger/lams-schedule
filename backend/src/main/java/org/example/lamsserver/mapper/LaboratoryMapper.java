package org.example.lamsserver.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.lamspojo.po.LaboratoryPO;

import java.util.List;

@Mapper
public interface LaboratoryMapper {

    String BASE_COLUMNS = "id, name, description, location, room_number, capacity, status, created_time ";

    @Select("select " + BASE_COLUMNS + "from laboratories where status = 1 order by id")
    List<LaboratoryPO> list();

    @Select("select " + BASE_COLUMNS + "from laboratories where id = #{id}")
    LaboratoryPO getById(Long id);

    @Select("select count(*) from laboratories where id = #{id} and status = 1")
    int countById(Long id);

    @Insert("insert into laboratories (name, description, location, room_number, capacity, status) " +
            "values (#{name}, #{description}, #{location}, #{roomNumber}, #{capacity}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LaboratoryPO lab);

    @Update("update laboratories set name = #{name}, description = #{description}, location = #{location}, " +
            "room_number = #{roomNumber}, capacity = #{capacity} where id = #{id}")
    int update(LaboratoryPO lab);

    @Update("update laboratories set status = 0 where id = #{id}")
    int deleteById(Long id);
}
