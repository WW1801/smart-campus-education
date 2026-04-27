package com.campus.education.config;

/**
 * 数据初始化配置类，负责启动时处理基础数据。
 */

import com.campus.education.entity.User;
import com.campus.education.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitConfig implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<User> users = userMapper.selectList(null);
        for (User user : users) {
            if (user.getPassword() != null && !user.getPassword().startsWith("$2a$10$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userMapper.updateById(user);
            }
        }
    }
}
