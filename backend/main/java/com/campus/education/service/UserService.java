package com.campus.education.service;

/**
 * 用户服务接口，定义用户相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {
    // 处理登录
    Map<String, Object> login(String username, String password);
    // 查找按用户名
    User findByUsername(String username);
    // 获取权限编码按角色编号
    List<String> getPermissionCodesByRoleId(String roleId);
}
