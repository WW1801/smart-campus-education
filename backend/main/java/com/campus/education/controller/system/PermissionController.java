package com.campus.education.controller.system;

/**
 * 权限控制器，负责处理权限相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessIdGenerator;
import com.campus.education.common.Result;
import com.campus.education.entity.Permission;
import com.campus.education.entity.RolePermission;
import com.campus.education.mapper.RolePermissionMapper;
import com.campus.education.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private BusinessIdGenerator businessIdGenerator;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    // 查询权限列表
    @GetMapping("/list")
    public Result<List<Permission>> list() {
        return Result.success(permissionService.list());
    }

    // 分页查询权限
    @GetMapping("/page")
    public Result<IPage<Permission>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(permissionService.page(new Page<>(current, size)));
    }

    // 添加权限
    @PostMapping
    public Result<Void> add(@RequestBody Permission permission) {
        if (permission.getPermissionId() == null || permission.getPermissionId().trim().isEmpty()) {
            permission.setPermissionId(businessIdGenerator.nextNumericId("permission", "permission_id"));
        }
        permissionService.save(permission);
        return Result.success("添加成功", null);
    }

    // 更新权限
    @PutMapping
    public Result<Void> update(@RequestBody Permission permission) {
        permissionService.updateById(permission);
        return Result.success("更新成功", null);
    }

    // 删除权限
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, id));
        permissionService.removeById(id);
        return Result.success("删除成功", null);
    }
}
