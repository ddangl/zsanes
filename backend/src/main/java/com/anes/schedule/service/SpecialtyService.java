package com.anes.schedule.service;

import com.anes.schedule.common.BusinessException;
import com.anes.schedule.dto.SpecialtyDtos.SpecialtyResp;
import com.anes.schedule.dto.SpecialtyDtos.SpecialtySaveReq;
import com.anes.schedule.entity.Specialty;
import com.anes.schedule.mapper.SpecialtyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 亚专科管理(轻量 CRUD,供人员档案双亚专科选择) */
@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyMapper specialtyMapper;

    public List<SpecialtyResp> listAll(boolean onlyActive) {
        LambdaQueryWrapper<Specialty> qw = new LambdaQueryWrapper<Specialty>()
                .eq(onlyActive, Specialty::getActive, true)
                .orderByAsc(Specialty::getSort);
        return specialtyMapper.selectList(qw).stream()
                .map(s -> new SpecialtyResp(s.getId(), s.getName(), s.getSort(), s.getActive()))
                .toList();
    }

    public void create(SpecialtySaveReq req) {
        assertNameUnique(req.name(), null);
        Specialty s = new Specialty();
        s.setName(req.name());
        s.setSort(req.sort() == null ? 0 : req.sort());
        s.setActive(true);
        specialtyMapper.insert(s);
    }

    public void update(Long id, SpecialtySaveReq req) {
        Specialty s = specialtyMapper.selectById(id);
        if (s == null) {
            throw BusinessException.badRequest("亚专科不存在:id=" + id);
        }
        assertNameUnique(req.name(), id);
        s.setName(req.name());
        s.setSort(req.sort() == null ? s.getSort() : req.sort());
        specialtyMapper.updateById(s);
    }

    public void delete(Long id) {
        Specialty s = specialtyMapper.selectById(id);
        if (s == null) {
            throw BusinessException.badRequest("亚专科不存在:id=" + id);
        }
        s.setActive(false);
        specialtyMapper.updateById(s);
    }

    private void assertNameUnique(String name, Long excludeId) {
        Long count = specialtyMapper.selectCount(new LambdaQueryWrapper<Specialty>()
                .eq(Specialty::getName, name)
                .ne(excludeId != null, Specialty::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("亚专科已存在:" + name);
        }
    }
}
