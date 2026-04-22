package com.campus.education.controller.teacher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.Teacher;
import com.campus.education.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @GetMapping("/list")
    public Result<List<Teacher>> list(@RequestParam(required = false) String departmentId) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Teacher::getDepartmentId, departmentId);
        }
        return Result.success(teacherService.list(wrapper));
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String teacherId,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String departmentId) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null && !teacherId.trim().isEmpty()) {
            wrapper.eq(Teacher::getTeacherId, teacherId);
        }
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Teacher::getName, name);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Teacher::getDepartmentId, departmentId);
        }
        Page<Teacher> pageParam = new Page<>(current, size);
        IPage<Teacher> result = teacherService.page(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success("查询成功", data);
    }

    @PostMapping
    public Result<Void> add(@RequestBody Teacher teacher) {
        teacherService.save(teacher);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Teacher teacher) {
        teacherService.updateById(teacher);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        teacherService.removeById(id);
        return Result.success("删除成功", null);
    }
}
