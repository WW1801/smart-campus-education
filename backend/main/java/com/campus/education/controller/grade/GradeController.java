package com.campus.education.controller.grade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.Grade;
import com.campus.education.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @GetMapping("/page")
    public Result<IPage<Grade>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String status) {
        Page<Grade> page = new Page<>(current, size);
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Grade::getSemesterId, semesterId);
        }
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.eq(Grade::getStudentId, studentId);
        }
        if (courseId != null && !courseId.trim().isEmpty()) {
            wrapper.eq(Grade::getCourseId, courseId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Grade::getStatus, status);
        }
        wrapper.orderByDesc(Grade::getCreatedAt);
        return Result.success(gradeService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public Result<Grade> getById(@PathVariable String id) {
        return Result.success(gradeService.getById(id));
    }

    @PostMapping
    public Result<Void> submit(@RequestBody Grade grade) {
        gradeService.submitGrade(grade);
        return Result.success("成绩录入成功", null);
    }

    @PostMapping("/batch")
    public Result<Void> batchSubmit(@RequestBody List<Grade> grades) {
        gradeService.batchSubmitGrades(grades);
        return Result.success("批量录入成功", null);
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable String id) {
        gradeService.approveGrade(id);
        return Result.success("审核通过", null);
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable String id, @RequestBody(required = false) Map<String, String> params) {
        String reason = params != null ? params.get("reason") : null;
        gradeService.rejectGrade(id, reason);
        return Result.success("已驳回", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Grade grade) {
        if (!"rejected".equals(grade.getStatus())) {
            return Result.badRequest("只能修改已驳回的成绩");
        }
        grade.setStatus("pending");
        grade.setIsPass(null);
        gradeService.updateById(grade);
        return Result.success("修改成功，已重新提交审核", null);
    }

    @GetMapping("/gpa/{studentId}")
    public Result<Map<String, Object>> gpa(
            @PathVariable String studentId,
            @RequestParam(required = false) String semesterId) {
        return Result.success(gradeService.calculateGpa(studentId, semesterId));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String classId) {
        return Result.success(gradeService.getStatistics(semesterId, courseId, classId));
    }
}
