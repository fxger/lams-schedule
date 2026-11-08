package org.example.lamsserver.controller.user;

import org.example.lamscommon.result.Result;
import org.example.lamspojo.dto.LaboratoryDTO;
import org.example.lamspojo.vo.LaboratoryVO;
import org.example.lamsserver.service.LaboratoryService;
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
@RequestMapping("/labs")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    @GetMapping
    public Result<List<LaboratoryVO>> list() {
        return Result.success(laboratoryService.list());
    }

    @PostMapping
    public Result<LaboratoryVO> save(@Valid @RequestBody LaboratoryDTO dto) {
        dto.setId(null);
        return Result.success(laboratoryService.save(dto));
    }

    @PutMapping("/{id}")
    public Result<LaboratoryVO> update(@PathVariable Long id, @Valid @RequestBody LaboratoryDTO dto) {
        dto.setId(id);
        return Result.success(laboratoryService.save(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        laboratoryService.delete(id);
        return Result.success();
    }
}
