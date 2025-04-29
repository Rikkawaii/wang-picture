package com.wang.wangpicture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.exception.ThrowUtils;
import com.wang.wangpicture.model.dto.space.SpaceAddRequest;
import com.wang.wangpicture.model.dto.space.SpaceQueryRequest;
import com.wang.wangpicture.model.entity.Space;
import com.wang.wangpicture.model.entity.SpaceUser;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.enums.SpaceLevelEnum;
import com.wang.wangpicture.model.enums.SpaceRoleEnum;
import com.wang.wangpicture.model.enums.SpaceTypeEnum;
import com.wang.wangpicture.model.vo.UserVO;
import com.wang.wangpicture.model.vo.space.SpaceVO;
import com.wang.wangpicture.service.SpaceService;
import com.wang.wangpicture.mapper.SpaceMapper;
import com.wang.wangpicture.service.SpaceUserService;
import com.wang.wangpicture.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author xwzy
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-03-03 16:39:58
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {
    private static final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpaceUserService spaceUserService;

    /**
     * 校验空间是否合法
     *
     * @param space 空间对象
     * @param add   true表示创建空间校验，false表示修改空间校验
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 要创建
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
            ThrowUtils.throwIf(spaceTypeEnum == null, ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        if (spaceTypeEnum != null && spaceTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
    }

    /**
     * 根据空间级别，自动填充限额
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }


    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 如果创建空间是，没有指定空间名称或空间级别或空间类型，则使用默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);

        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        // 数据校验
        this.validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验（用户只能创建普通级别的空间）
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 针对用户进行加锁（一个用户仅能有一个私有空间）  分布式场景下可能无效
        ReentrantLock lock = lockMap.computeIfAbsent(userId, key -> new ReentrantLock()); // 获取锁对象
        try {
            boolean flag = lock.tryLock(2L, TimeUnit.SECONDS);// 获取锁
            // 2秒内没有获取到锁，说明同一用户有其他线程正在创建空间，抛出异常
            ThrowUtils.throwIf(!flag, ErrorCode.OPERATION_ERROR, "当前正在创建空间，请稍后再试");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 获取到锁，开始创建空间
        Long newSpaceId = transactionTemplate.execute(status -> {
            // 事务开始，保证每个用户仅有一个私有空间
            if (!userService.isAdmin(loginUser)) {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
            }
            // 写入数据库
            boolean result = this.save(space);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 如果是团队空间，关联新增团队成员记录
            if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                SpaceUser spaceUser = new SpaceUser();
                spaceUser.setSpaceId(space.getId());
                spaceUser.setUserId(userId);
                spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                result = spaceUserService.save(spaceUser);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
            }
            // 返回新写入的数据 id
            return space.getId();
        });
        lock.unlock(); // 释放锁
        lockMap.compute(userId, (key, value) -> {
            if (value != null && value.tryLock()) {
                try {
                    return null; // 可以安全删除锁
                } finally {
                    value.unlock();
                }
            }
            // 如果锁为null或者被占用，则不删除锁
            return value;
        });
        // 返回结果是包装类，可以做一些处理
        return Optional.ofNullable(newSpaceId).orElse(-1L);
    }

    /**
     * 删除空间时的权限校验
     * @param loginUser
     * @param oldSpace
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space oldSpace) {
        // 仅本人或管理员可编辑
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 构造查询空间条件
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public Wrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();

        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(spaceLevel != null, "spaceLevel", spaceLevel);
        queryWrapper.eq(spaceType != null, "spaceType", spaceType);
        return queryWrapper;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }


}




