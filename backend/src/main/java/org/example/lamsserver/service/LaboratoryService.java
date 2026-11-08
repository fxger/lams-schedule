package org.example.lamsserver.service;

import org.example.lamspojo.dto.LaboratoryDTO;
import org.example.lamspojo.vo.LaboratoryVO;

import java.util.List;

public interface LaboratoryService {

    List<LaboratoryVO> list();

    LaboratoryVO save(LaboratoryDTO dto);

    void delete(Long id);
}
