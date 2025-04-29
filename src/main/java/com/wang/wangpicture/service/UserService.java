package com.wang.wangpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.wangpicture.model.dto.user.UserQueryRequest;
import com.wang.wangpicture.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wang.wangpicture.model.vo.LoginUserVO;
import com.wang.wangpicture.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xwzy
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-24 21:46:29
*/
public interface UserService extends IService<User> {
    long userRegister(String userAccount, String userPassword, String checkPassword);

    long userEmailRegister(String userAccount, String userPassword, String checkPassword, String email, String code);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    LoginUserVO getLoginUserVO(User user);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    boolean userLogout(HttpServletRequest request);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    String getEncryptPassword(String defaultPassword);

    boolean isAdmin(User loginUser);

    void sendRegisterCode(String email);
}
