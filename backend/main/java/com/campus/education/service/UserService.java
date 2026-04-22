package com.campus.education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {
    Map<String, Object> login(String username, String password);
    User findByUsername(String username);
    List<String> getPermissionCodesByRoleId(String roleId);
}
