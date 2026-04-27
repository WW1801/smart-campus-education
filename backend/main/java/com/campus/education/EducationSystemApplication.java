package com.campus.education;

/**
 * 教育管理系统后端启动类，负责启动 Spring Boot 应用。
 */

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class, org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class})
@ComponentScan(basePackages = "com.campus.education")
@MapperScan(basePackages = "com.campus.education.mapper")
public class EducationSystemApplication {
    // 启动应用
    public static void main(String[] args) {
        SpringApplication.run(EducationSystemApplication.class, args);
    }
}
