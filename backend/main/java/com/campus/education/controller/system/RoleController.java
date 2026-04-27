package com.campus.education.controller.system;

/**
 * 角色控制器，负责处理角色相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessIdGenerator;
import com.campus.education.common.Result;
import com.campus.education.entity.Role;
import com.campus.education.entity.RolePermission;
import com.campus.education.mapper.RolePermissionMapper;
import com.campus.education.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private BusinessIdGenerator businessIdGenerator;

    // 查询角色列表
    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.list());
    }

    // 分页查询角色
    @GetMapping("/page")
    public Result<IPage<Role>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(roleService.page(new Page<>(current, size)));
    }

    // 获取角色详情
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable String id) {
        return Result.success(roleService.getById(id));
    }

    // 添加角色
    @PostMapping
    public Result<Void> add(@RequestBody Role role) {
        if (role.getRoleId() == null || role.getRoleId().trim().isEmpty()) {
            role.setRoleId(businessIdGenerator.nextNumericId("role", "role_id"));
        }
        roleService.save(role);
        return Result.success("添加成功", null);
    }

    // 更新角色
    @PutMapping
    public Result<Void> update(@RequestBody Role role) {
        roleService.updateById(role);
        return Result.success("更新成功", null);
    }

    // 删除角色
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        roleService.removeById(id);
        return Result.success("删除成功", null);
    }

    // 获取权限
    @GetMapping("/{id}/permissions")
    public Result<List<String>> getPermissions(@PathVariable String id) {
        List<RolePermission> list = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        List<String> permissionIds = list.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        return Result.success(permissionIds);
    }

    // 分配权限
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable String id, @RequestBody List<String> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> rolePermissions = permissionIds.stream().map(pid -> {
                RolePermission rp = new RolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(pid);
                return rp;
            }).collect(Collectors.toList());
            rolePermissions.forEach(rolePermissionMapper::insert);
        }
        return Result.success("权限分配成功", null);
    }
}
