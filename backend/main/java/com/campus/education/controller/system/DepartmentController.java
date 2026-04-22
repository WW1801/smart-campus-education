package com.campus.education.controller.system;

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

    @GetMapping("/list")
    public Result<List<Department>> list() {
        return Result.success(departmentService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Department department) {
        departmentService.save(department);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Department department) {
        departmentService.updateById(department);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        departmentService.removeById(id);
        return Result.success("删除成功", null);
    }
}
