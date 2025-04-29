package com.wang.wangpicture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wang.wangpicture.constant.UserConstant;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.manager.auth.StpKit;
import com.wang.wangpicture.manager.email.EmailSendManager;
import com.wang.wangpicture.model.dto.user.UserQueryRequest;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.enums.UserRoleEnum;
import com.wang.wangpicture.model.vo.LoginUserVO;
import com.wang.wangpicture.model.vo.UserVO;
import com.wang.wangpicture.service.UserService;
import com.wang.wangpicture.mapper.UserMapper;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.wang.wangpicture.constant.RedisConstant.*;
import static com.wang.wangpicture.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author xwzy
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-02-24 21:46:29
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private EmailSendManager emailSendManager;

    //todo: 考虑增加更多注册方式
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        validateUserAccount(userAccount, userPassword, checkPassword);
        // 2. 密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }
    public void validateUserAccount(String userAccount, String userPassword, String checkPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查用户账号是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
    }

    /**
     * 用户邮箱注册
     *
     * @param email
     * @param code
     * @return
     */
    @Override
    public long userEmailRegister(String userAccount, String userPassword, String checkPassword, String email, String code) {
        // 1. 校验参数
        validateUserAccount(userAccount, userPassword, checkPassword);
        // 2.校验邮箱是否已经注册过
        synchronized (email.intern()) {
            // 账户不能重复
            boolean isExists = this.query().eq("email", email).exists();
            if(isExists){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已经注册过!!!");
            }
            // 3. 插入数据
            String encryptPassword = getEncryptPassword(userPassword);
            User user = new User();
            user.setEmail(email);
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName("无名");
            user.setUserRole(UserRoleEnum.USER.getValue());
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    // todo: 待更改成JWT+Redis实现
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        // note: 已修改为链式调用
        User user = this.query().eq("userAccount", userAccount).eq("userPassword", encryptPassword).one();
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * todo: 优化代码，提高性能
     * 获取登录用户信息
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库中查询（追求性能的话可以注释，直接返回上述结果）
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获得登录用户脱敏信息
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
    

    @Override
    public void sendRegisterCode(String email) {
        // 定义锁的缓存键（过期时间 60秒，防止长时间锁住）
        String emailSendLockKey = EMAIL_REGISTER_CODE_LOCK_KEY + email;

        // 尝试获取分布式锁，设置过期时间 60秒，避免死锁
        Boolean isLockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(emailSendLockKey, "LOCK", EMAIL_REGISTER_CODE_LOCK_EXPIRE, TimeUnit.SECONDS);

        // 如果获取锁失败，表示有其他请求正在发送验证码，拒绝继续执行
        if (!Boolean.TRUE.equals(isLockAcquired)) {
            throw new IllegalArgumentException("验证码发送过于频繁，请稍后再试！");
        }
        try {
            // 生成验证码
            String code = RandomUtil.randomNumbers(4);
            // 设置验证码到Redis缓存（60秒过期）
            stringRedisTemplate.opsForValue().set(EMAIL_REGISTER_CODE_KEY + email, code, EMAIL_REGISTER_CODE_LOCK_EXPIRE, TimeUnit.SECONDS);
            // 发送邮件验证码
            emailSendManager.sendRegisterEmail(email, code);
        } finally {
            // 释放锁
            stringRedisTemplate.delete(emailSendLockKey);
        }
    }


    private boolean emailCodeValid(String email, String code) {
        String codeCheck = stringRedisTemplate.opsForValue().get(EMAIL_REGISTER_CODE_KEY + email);
        // 验证码过期或没发送
        if (StringUtil.isBlank(codeCheck)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式或邮箱验证码错误!!!");
        }
        // 验证码错误
        if (!codeCheck.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式或邮箱验证码错误!!!");
        }
        return true;
    }

    /**
     * 加密密码
     *
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        final String salt = "wang";
        return DigestUtil.md5Hex((salt + userPassword).getBytes());
    }

}




