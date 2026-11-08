package org.example.lamsserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.lamspojo.dto.LaboratoryDTO;
import org.example.lamspojo.po.LaboratoryPO;
import org.example.lamspojo.vo.LaboratoryVO;
import org.example.lamsserver.exception.BizException;
import org.example.lamsserver.mapper.LaboratoryMapper;
import org.example.lamsserver.mapper.ScheduleMapper;
import org.example.lamsserver.service.LaboratoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LaboratoryServiceImpl implements LaboratoryService {

    private final LaboratoryMapper laboratoryMapper;
    private final ScheduleMapper scheduleMapper;

    public LaboratoryServiceImpl(LaboratoryMapper laboratoryMapper, ScheduleMapper scheduleMapper) {
        this.laboratoryMapper = laboratoryMapper;
        this.scheduleMapper = scheduleMapper;
    }

    @Override
    public List<LaboratoryVO> list() {
        return laboratoryMapper.list().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public LaboratoryVO save(LaboratoryDTO dto) {
        LaboratoryPO po = new LaboratoryPO();
        po.setId(dto.getId());
        po.setName(dto.getName());
        po.setDescription(dto.getDescription());
        po.setLocation(dto.getLocation());
        po.setRoomNumber(dto.getRoomNumber());
        po.setCapacity(dto.getCapacity());
        if (po.getId() == null) {
            laboratoryMapper.insert(po);
        } else {
            laboratoryMapper.update(po);
        }
        return toVO(laboratoryMapper.getById(po.getId()));
    }

    @Override
    public void delete(Long id) {
        int used = scheduleMapper.countByLabId(id);
        if (used > 0) {
            throw new BizException("该实验室还有 " + used + " 条排课记录，请先删除相关排课");
        }
        laboratoryMapper.deleteById(id);
        log.info("实验室已删除: {}", id);
    }

    private LaboratoryVO toVO(LaboratoryPO po) {
        LaboratoryVO vo = new LaboratoryVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setLocation(po.getLocation());
        vo.setRoomNumber(po.getRoomNumber());
        vo.setCapacity(po.getCapacity());
        vo.setStatus(po.getStatus());
        return vo;
    }
}
