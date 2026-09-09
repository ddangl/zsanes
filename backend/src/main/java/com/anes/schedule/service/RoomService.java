package com.anes.schedule.service;

import com.anes.schedule.common.BusinessException;
import com.anes.schedule.common.ErrorCode;
import com.anes.schedule.common.PageResult;
import com.anes.schedule.dto.RoomDtos.RoomCreateReq;
import com.anes.schedule.dto.RoomDtos.RoomQuery;
import com.anes.schedule.dto.RoomDtos.RoomResp;
import com.anes.schedule.dto.RoomDtos.RoomUpdateReq;
import com.anes.schedule.entity.Room;
import com.anes.schedule.mapper.RoomMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/** 手术室房间管理(样板模块:后续模块照此结构填空) */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;

    public PageResult<RoomResp> page(RoomQuery query) {
        LambdaQueryWrapper<Room> qw = new LambdaQueryWrapper<Room>()
                .eq(StringUtils.hasText(query.area()), Room::getArea, query.area())
                .like(StringUtils.hasText(query.keyword()), Room::getCode, query.keyword())
                .orderByAsc(Room::getSort);
        Page<Room> page = roomMapper.selectPage(Page.of(query.page(), query.size()), qw);
        List<RoomResp> list = page.getRecords().stream().map(RoomResp::from).toList();
        return PageResult.of(page.getTotal(), list);
    }

    /** 区域去重列表(供筛选下拉) */
    public List<String> areas() {
        return roomMapper.selectObjs(new LambdaQueryWrapper<Room>()
                        .select(Room::getArea).isNotNull(Room::getArea).groupBy(Room::getArea)
                        .orderByAsc(Room::getSort))
                .stream().map(String::valueOf).toList();
    }

    public void create(RoomCreateReq req) {
        assertCodeUnique(req.code(), null);
        Room room = new Room();
        applyCreate(room, req);
        roomMapper.insert(room);
    }

    public void update(Long id, RoomUpdateReq req) {
        Room room = requireRoom(id);
        assertCodeUnique(req.code(), id);
        room.setCode(req.code());
        room.setArea(req.area());
        room.setStarred(Boolean.TRUE.equals(req.starred()));
        room.setSchedulable(req.schedulable() == null || req.schedulable());
        room.setSort(req.sort() == null ? 0 : req.sort());
        room.setActive(req.active() == null || req.active());
        roomMapper.updateById(room);
    }

    /** 停用(逻辑删除,保留历史引用) */
    public void delete(Long id) {
        Room room = requireRoom(id);
        room.setActive(false);
        roomMapper.updateById(room);
    }

    public RoomResp get(Long id) {
        return RoomResp.from(requireRoom(id));
    }

    private Room requireRoom(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "房间不存在:id=" + id);
        }
        return room;
    }

    private void applyCreate(Room room, RoomCreateReq req) {
        room.setCode(req.code());
        room.setArea(req.area());
        room.setStarred(Boolean.TRUE.equals(req.starred()));
        room.setSchedulable(req.schedulable() == null || req.schedulable());
        room.setSort(req.sort() == null ? 0 : req.sort());
        room.setActive(true);
    }

    private void assertCodeUnique(String code, Long excludeId) {
        Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getCode, code)
                .ne(excludeId != null, Room::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("房号已存在:" + code);
        }
    }
}
