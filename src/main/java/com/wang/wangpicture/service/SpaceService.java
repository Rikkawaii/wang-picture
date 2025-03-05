package com.wang.wangpicture.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.wangpicture.model.dto.space.SpaceAddRequest;
import com.wang.wangpicture.model.dto.space.SpaceQueryRequest;
import com.wang.wangpicture.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author xwzy
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-03 16:39:58
*/
public interface SpaceService extends IService<Space> {

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    void checkSpaceAuth(User loginUser, Space oldSpace);

    Wrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);
}
