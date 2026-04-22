package com.campus.education.controller;

import com.campus.education.common.Result;
import com.campus.education.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private UserService userService;

    @PostMapping
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Result.badRequest("用户名和密码不能为空");
        }
        Map<String, Object> loginResult = userService.login(username, password);
        return Result.success("登录成功", loginResult);
    }
}
