package com.wang.wangpicture;

import com.wang.wangpicture.manager.email.EmailSendManager;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.wang.wangpicture.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true) // 使得可以在业务逻辑中访问到当前的代理对象
public class WangPictureApplication {
    public static void main(String[] args) {
        SpringApplication.run(WangPictureApplication.class, args);
    }
}
