package com.campus.education.config;

import com.campus.education.entity.User;
import com.campus.education.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (admin != null) {
            String testEncode = passwordEncoder.encode("123456");
            if (!passwordEncoder.matches("123456", admin.getPassword())) {
                admin.setPassword(testEncode);
                userMapper.updateById(admin);
                log.info("Reset admin password to 123456");
            }
            if (!passwordEncoder.matches("123456", admin.getPassword())) {
                log.error("BCrypt password matching still fails after reset!");
            } else {
                log.info("Admin password verified: 123456");
            }
        }

        User student1 = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "student1"));
        if (student1 != null && !passwordEncoder.matches("123456", student1.getPassword())) {
            student1.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(student1);
            log.info("Reset student1 password to 123456");
        }

        User teacher1 = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "teacher1"));
        if (teacher1 != null && !passwordEncoder.matches("123456", teacher1.getPassword())) {
            teacher1.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(teacher1);
            log.info("Reset teacher1 password to 123456");
        }

        User jwc = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "jwc"));
        if (jwc != null && !passwordEncoder.matches("123456", jwc.getPassword())) {
            jwc.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(jwc);
            log.info("Reset jwc password to 123456");
        }

        User dept = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "dept"));
        if (dept != null && !passwordEncoder.matches("123456", dept.getPassword())) {
            dept.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(dept);
            log.info("Reset dept password to 123456");
        }
    }
}
