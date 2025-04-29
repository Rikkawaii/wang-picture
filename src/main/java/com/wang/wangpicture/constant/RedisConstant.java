package com.wang.wangpicture.constant;

import java.util.concurrent.TimeUnit;

public interface RedisConstant {

    String EMAIL_REGISTER_CODE_KEY = "wangpicture:emailRegisterCode:";
    String EMAIL_REGISTER_CODE_LOCK_KEY = "wangpicture:emailRegisterCode:lock:";
    long EMAIL_REGISTER_CODE_LOCK_EXPIRE = 60;

    // 分页查询图片缓存key
    String PAGE_CACHE_KEY = "wangpicture:listPictureVOPage:";
    // 分页查询图片缓存过期时间
    long PAGE_CACHE_EXPIRE = 300;
    // 分页查询图片分布式锁
    String PAGE_CACHE_LOCK_KEY = "wangpicture:listPictureVOPage:lock:";
    // 时间单位（秒）
    TimeUnit SECONDS_UNIT = TimeUnit.SECONDS;
}
