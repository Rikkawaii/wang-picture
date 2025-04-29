package com.wang.wangpicture.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系
 * 添加 @Component 注解的目的是确保静态属性 DEFAULT 和 SPACE 被初始化
 */
@Component
public class StpKit {
    public static final String SPACE_TYPE = "space";
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);
}
