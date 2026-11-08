package org.example.lamsserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.lamspojo.dto.CourseDTO;
import org.example.lamspojo.po.CoursePO;
import org.example.lamspojo.vo.CourseVO;
import org.example.lamsserver.exception.BizException;
import org.example.lamsserver.mapper.CourseMapper;
import org.example.lamsserver.mapper.CourseStudentMapper;
import org.example.lamsserver.mapper.ScheduleMapper;
import org.example.lamsserver.service.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ScheduleMapper scheduleMapper;

    public CourseServiceImpl(CourseMapper courseMapper,
                             CourseStudentMapper courseStudentMapper,
                             ScheduleMapper scheduleMapper) {
        this.courseMapper = courseMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.scheduleMapper = scheduleMapper;
    }

    @Override
    public List<CourseVO> list() {
        return courseMapper.list().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public CourseVO getById(Long id) {
        CoursePO po = courseMapper.getById(id);
        if (po == null) {
            throw new BizException("课程不存在");
        }
        return toVO(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO save(CourseDTO dto) {
        CoursePO po = new CoursePO();
        po.setId(dto.getId());
        po.setName(dto.getName());
        po.setCode(dto.getCode());
        po.setSemester(dto.getSemester());
        po.setCredits(dto.getCredits() == null ? 3 : dto.getCredits());
        po.setWeeks(dto.getWeeks());
        po.setPeriods(dto.getPeriods());
        po.setDescription(dto.getDescription());

        if (po.getId() == null) {
            courseMapper.insert(po);
        } else {
            courseMapper.update(po);
        }

        courseStudentMapper.deleteByCourseId(po.getId());
        if (dto.getStudentIds() != null) {
            for (Long studentId : dto.getStudentIds()) {
                courseStudentMapper.insert(po.getId(), studentId);
            }
        }
        log.info("课程保存成功: {} (id={})", po.getName(), po.getId());
        return getById(po.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CoursePO po = courseMapper.getById(id);
        if (po == null) {
            throw new BizException("课程不存在");
        }
        scheduleMapper.deleteByCourseId(id);
        courseStudentMapper.deleteByCourseId(id);
        courseMapper.deleteById(id);
        log.info("课程已删除: {}", po.getName());
    }

    private CourseVO toVO(CoursePO po) {
        CourseVO vo = new CourseVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setCode(po.getCode());
        vo.setSemester(po.getSemester());
        vo.setCredits(po.getCredits());
        vo.setWeeks(po.getWeeks());
        vo.setPeriods(po.getPeriods());
        vo.setDescription(po.getDescription());
        vo.setStudentIds(new ArrayList<>(courseStudentMapper.listStudentIds(po.getId())));
        vo.setStudentCount(po.getStudentCount() == null ? vo.getStudentIds().size() : po.getStudentCount());
        return vo;
    }
}
