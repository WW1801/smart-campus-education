package com.campus.education.controller.system;

/**
 * 院系控制器，负责处理院系相关接口请求。
 */

import com.campus.education.common.Result;
import com.campus.education.entity.Department;
import com.campus.education.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    // 查询院系列表
    @GetMapping("/list")
    public Result<List<Department>> list() {
        return Result.success(departmentService.list());
    }

    // 添加院系
    @PostMapping
    public Result<Void> add(@RequestBody Department department) {
        departmentService.save(department);
        return Result.success("添加成功", null);
    }

    // 更新院系
    @PutMapping
    public Result<Void> update(@RequestBody Department department) {
        departmentService.updateById(department);
        return Result.success("更新成功", null);
    }

    // 删除院系
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        departmentService.removeById(id);
        return Result.success("删除成功", null);
    }
}
