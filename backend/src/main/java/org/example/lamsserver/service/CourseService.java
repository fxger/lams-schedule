package org.example.lamsserver.service;

import org.example.lamspojo.dto.CourseDTO;
import org.example.lamspojo.vo.CourseVO;

import java.util.List;

public interface CourseService {

    List<CourseVO> list();

    CourseVO getById(Long id);

    CourseVO save(CourseDTO courseDTO);

    void delete(Long id);
}
