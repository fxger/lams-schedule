package org.example.lamsserver.controller.user;

import org.example.lamscommon.result.Result;
import org.example.lamspojo.dto.CourseDTO;
import org.example.lamspojo.vo.CourseVO;
import org.example.lamsserver.service.CourseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Result<List<CourseVO>> list() {
        return Result.success(courseService.list());
    }

    @GetMapping("/{id}")
    public Result<CourseVO> getById(@PathVariable Long id) {
        return Result.success(courseService.getById(id));
    }

    @PostMapping
    public Result<CourseVO> save(@Valid @RequestBody CourseDTO dto) {
        dto.setId(null);
        return Result.success(courseService.save(dto));
    }

    @PutMapping("/{id}")
    public Result<CourseVO> update(@PathVariable Long id, @Valid @RequestBody CourseDTO dto) {
        dto.setId(id);
        return Result.success(courseService.save(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.success();
    }
}
