package com.campus.education.service.impl;

/**
 * 用户服务实现类，负责处理用户相关业务逻辑。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.common.JwtUtils;
import com.campus.education.entity.Permission;
import com.campus.education.entity.Role;
import com.campus.education.entity.RolePermission;
import com.campus.education.entity.User;
import com.campus.education.mapper.PermissionMapper;
import com.campus.education.mapper.RoleMapper;
import com.campus.education.mapper.RolePermissionMapper;
import com.campus.education.mapper.UserMapper;
import com.campus.education.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    // 处理登录
    @Override
    public Map<String, Object> login(String username, String password) {
        User user = findByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        user.setLastLogin(LocalDateTime.now());
        this.updateById(user);

        String token = jwtUtils.generateToken(user.getUserId(), user.getUsername(), user.getRoleId());

        Role role = roleMapper.selectById(user.getRoleId());
        List<String> permissionCodes = getPermissionCodesByRoleId(user.getRoleId());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("roleId", user.getRoleId());
        data.put("roleName", role != null ? role.getName() : "");
        data.put("relatedId", user.getRelatedId());
        data.put("permissions", permissionCodes);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", data);
        return result;
    }

    // 查找按用户名
    @Override
    public User findByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    // 获取权限编码按角色编号
    @Override
    public List<String> getPermissionCodesByRoleId(String roleId) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
        return permissions.stream().map(Permission::getCode).collect(Collectors.toList());
    }
}
