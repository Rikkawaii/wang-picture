package com.wang.wangpicture.model.dto.user;


import lombok.Getter;

import java.io.Serializable;

/**
 * 用户注册请求类
 */
@Getter
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    // 定义一个私有字符串变量username，用于存储用户名
    private String userAccount;
    // 定义一个私有字符串变量password，用于存储密码
    private String userPassword;
    // 定义一个私有字符串变量checkPassword，用于存储确认密码
    private String checkPassword;
    // 定义一个私有字符串变量email，用于存储邮箱
    private String email;
    // 定义一个私有字符串变量code,用于存储验证码
    private String code;
}
