package com.wang.wangpicture.aop;

import com.wang.wangpicture.annotation.AuthCheck;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.enums.UserRoleEnum;
import com.wang.wangpicture.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取必须的角色值
        String mustRole = authCheck.mustRole();
        // 根据角色值获取对应的枚举类型
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // a.如果必须的角色枚举为空，则直接放行
        if (mustRoleEnum == null) {
            joinPoint.proceed();
        }
        // b.如果必须的角色枚举不为空，则说明要求用户是特定身份
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        // 根据用户角色值获取对应的枚举类型
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 如果用户角色枚举为空，则抛出无权限异常
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 如果必须的角色是管理员且当前用户不是管理员，则抛出无权限异常
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 如果权限验证通过，则继续执行原方法
        return joinPoint.proceed();
    }
}
