package com.wang.wangpicture.controller;

import cn.hutool.core.util.ObjectUtil;
import com.wang.wangpicture.common.BaseResponse;
import com.wang.wangpicture.common.DeleteRequest;
import com.wang.wangpicture.common.ResultUtils;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.exception.ThrowUtils;
import com.wang.wangpicture.manager.auth.annotation.SaSpaceCheckPermission;
import com.wang.wangpicture.manager.auth.model.SpaceUserPermissionConstant;
import com.wang.wangpicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.wang.wangpicture.model.dto.spaceuser.SpaceUserEditRequest;
import com.wang.wangpicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.wang.wangpicture.model.entity.SpaceUser;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.vo.spaceuser.SpaceUserVO;
import com.wang.wangpicture.service.SpaceUserService;
import com.wang.wangpicture.service.UserService;
import com.wang.wangpicture.utils.JwtUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ReentrantLock lock = new ReentrantLock();
    /**
     * 发送邀请
     * @param spaceUserAddRequest
     * @return 是否发送成功
     */
    @PostMapping("/invite/send")
    // todo: 目前只有公共空间管理员可以邀请别人
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> sendInvite(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        User loginUser = userService.getLoginUser(request);
        spaceUserService.sendInvite(spaceUserAddRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 验证邀请凭证
     * @param token
     * @param request
     * @return
     */
    @GetMapping("/invite/validate")
    public BaseResponse<Boolean> validateInvite(@RequestParam("token") String token, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);// 未登录会throw异常,重新登录后还会重定向到这个接口
        JwtUtils.checkTokenForInvite(token);// 如果验证失败,会抛出具体异常
        SpaceUserAddRequest spaceUserAddRequest  = JwtUtils.getSpaceUserAddRequestByToken(token);
        Long userId = spaceUserAddRequest.getUserId();
        ThrowUtils.throwIf(!userId.equals(loginUser.getId()), ErrorCode.PARAMS_ERROR, "当前登录用户非应邀用户");
        return ResultUtils.success(true);
    }

    /**
     * 接受邀请
     * @param token
     * @param request
     * @return 空间用户id
     */
    @PostMapping("/invite/accept")
    public BaseResponse<Long> acceptInvite(@RequestParam("token") String token, HttpServletRequest request) {
        boolean locked = false;
        Long spaceUserId = null;
        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "系统繁忙");
            User loginUser = userService.getLoginUser(request);// 未登录会throw异常,重新登录后还会重定向到这个接口
            if (stringRedisTemplate.hasKey(token)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该token已被使用");
            }
            SpaceUserAddRequest spaceUserAddRequest = JwtUtils.getSpaceUserAddRequestByToken(token);
            spaceUserId = spaceUserService.addSpaceUser(spaceUserAddRequest);
            // token使用将其放入redis中,有效期为2小时,防止重复使用
            stringRedisTemplate.opsForValue().set(token, "USED", 7200000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return ResultUtils.success(spaceUserId);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);
        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }
}
