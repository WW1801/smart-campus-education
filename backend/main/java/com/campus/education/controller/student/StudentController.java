package com.campus.education.controller.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.Student;
import com.campus.education.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/list")
    public Result<List<Student>> list(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (classId != null && !classId.trim().isEmpty()) {
            wrapper.eq(Student::getClassId, classId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Student::getStatus, status);
        }
        wrapper.orderByAsc(Student::getStudentId);
        return Result.success(studentService.list(wrapper));
    }

    @GetMapping("/page")
    public Result<IPage<Student>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String majorId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status) {
        Page<Student> page = new Page<>(current, size);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.like(Student::getStudentId, studentId);
        }
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Student::getName, name);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Student::getDepartmentId, departmentId);
        }
        if (majorId != null && !majorId.trim().isEmpty()) {
            wrapper.eq(Student::getMajorId, majorId);
        }
        if (classId != null && !classId.trim().isEmpty()) {
            wrapper.eq(Student::getClassId, classId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Student::getStatus, status);
        }
        wrapper.orderByDesc(Student::getCreatedAt);
        return Result.success(studentService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public Result<Student> getById(@PathVariable String id) {
        return Result.success(studentService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Student student) {
        student.setStatus("active");
        studentService.save(student);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Student student) {
        studentService.updateById(student);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        studentService.removeById(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable String id, @RequestBody Map<String, String> params) {
        String targetStatus = params.get("status");
        String reason = params.get("reason");
        if (targetStatus == null || targetStatus.trim().isEmpty()) {
            return Result.badRequest("目标状态不能为空");
        }
        studentService.changeStatus(id, targetStatus, reason);
        return Result.success("学籍状态变更成功", null);
    }
}
