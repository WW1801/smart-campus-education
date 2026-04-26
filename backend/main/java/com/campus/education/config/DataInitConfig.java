package com.campus.education.config;

import com.campus.education.entity.User;
import com.campus.education.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitConfig.class);

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
                log.info("Encoded password for user: {}", user.getUsername());
            }
        }
    }
}
