package com.wang.wangpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.wangpicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.wang.wangpicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.wang.wangpicture.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.vo.spaceuser.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xwzy
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-03-08 12:10:57
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean b);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    void sendInvite(SpaceUserAddRequest spaceUserAddRequest, User loginUser);
}
