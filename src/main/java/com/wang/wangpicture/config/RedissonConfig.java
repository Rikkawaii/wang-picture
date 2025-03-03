package com.wang.wangpicture.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 */
@Configuration
public class RedissonConfig {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.password}")
    private String password;
    //配置
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        //目前redis是单机状态，直接设置地址
        config.useSingleServer().setAddress("redis://"+host+":"+port).setPassword(password);
        //创建RedissonClient对象
        return Redisson.create(config);
    }
}
