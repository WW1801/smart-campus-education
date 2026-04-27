package com.campus.education.controller.course;

/**
 * 课程控制器，负责处理课程相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.Course;
import com.campus.education.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/list")
    public Result<List<Course>> list(@RequestParam(required = false) String departmentId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Course::getDepartmentId, departmentId);
        }
        return Result.success(courseService.list(wrapper));
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String code,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String departmentId,
                                            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (code != null && !code.trim().isEmpty()) {
            wrapper.eq(Course::getCode, code);
        }
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Course::getName, name);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Course::getDepartmentId, departmentId);
        }
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(Course::getType, type);
        }
        Page<Course> pageParam = new Page<>(current, size);
        IPage<Course> result = courseService.page(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success("查询成功", data);
    }

    // 添加课程
    @PostMapping
    public Result<Void> add(@RequestBody Course course) {
        courseService.save(course);
        return Result.success("添加成功", null);
    }

    // 更新课程
    @PutMapping
    public Result<Void> update(@RequestBody Course course) {
        courseService.updateById(course);
        return Result.success("更新成功", null);
    }

    // 删除课程
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        courseService.removeById(id);
        return Result.success("删除成功", null);
    }
}
