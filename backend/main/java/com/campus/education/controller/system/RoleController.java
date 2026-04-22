package com.campus.education.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.list());
    }

    @GetMapping("/page")
    public Result<IPage<Role>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(roleService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable String id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Role role) {
        roleService.save(role);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Role role) {
        roleService.updateById(role);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        roleService.removeById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id}/permissions")
    public Result<List<String>> getPermissions(@PathVariable String id) {
        List<RolePermission> list = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        List<String> permissionIds = list.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        return Result.success(permissionIds);
    }

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
