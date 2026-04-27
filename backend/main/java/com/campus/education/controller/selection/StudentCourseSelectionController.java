package com.campus.education.controller.selection;

/**
 * 学生选课控制器，负责处理学生选课相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessException;
import com.campus.education.common.Result;
import com.campus.education.common.StudentAccessGuard;
import com.campus.education.entity.StudentCourseSelection;
import com.campus.education.service.StudentCourseSelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/selection")
public class StudentCourseSelectionController {

    @Autowired
    private StudentCourseSelectionService selectionService;

    @Autowired
    private StudentAccessGuard studentAccessGuard;

    // [迭代补充] 选课接口：含先修课程+时间冲突+容量校验
    @PostMapping("/select")
    public Result<StudentCourseSelection> selectCourse(@RequestBody Map<String, String> params,
                                                       Authentication authentication) {
        String studentId = studentAccessGuard.currentStudentId(authentication, params.get("studentId"));
        String scheduleId = params.get("scheduleId");
        if (scheduleId == null || scheduleId.trim().isEmpty()) {
            return Result.badRequest("学号和排课ID不能为空");
        }
        StudentCourseSelection selection = selectionService.selectCourse(studentId, scheduleId);
        return Result.success("选课成功", selection);
    }

    // [迭代补充] 退选接口
    @PutMapping("/{id}/drop")
    public Result<Void> dropCourse(@PathVariable String id, Authentication authentication) {
        StudentCourseSelection selection = selectionService.getById(id);
        if (selection == null) {
            throw new BusinessException("selection not found");
        }
        studentAccessGuard.currentStudentId(authentication, selection.getStudentId());
        selectionService.dropCourse(id);
        return Result.success("退选成功", null);
    }

    // [迭代补充] 查询我的选课列表
    @GetMapping("/my-courses")
    public Result<List<StudentCourseSelection>> myCourses(
            @RequestParam String studentId,
            @RequestParam(required = false) String semesterId,
            Authentication authentication) {
        studentId = studentAccessGuard.currentStudentId(authentication, studentId);
        return Result.success(selectionService.getMyCourses(studentId, semesterId));
    }

    // [迭代补充] 查询我的课表（含课程名/教师名/时间）
    @GetMapping("/my-schedule")
    public Result<List<Map<String, Object>>> mySchedule(
            @RequestParam String studentId,
            @RequestParam(required = false) String semesterId,
            Authentication authentication) {
        studentId = studentAccessGuard.currentStudentId(authentication, studentId);
        return Result.success(selectionService.getMySchedule(studentId, semesterId));
    }

    // 处理课表
    @GetMapping("/timetable")
    public Result<List<Map<String, Object>>> timetable(
            @RequestParam String studentId,
            @RequestParam(required = false) String semesterId,
            Authentication authentication) {
        studentId = studentAccessGuard.currentStudentId(authentication, studentId);
        return Result.success(selectionService.getStudentTimetable(studentId, semesterId));
    }

    // [迭代补充] 查询可选课程列表（含先修/冲突/容量标记）
    @GetMapping("/available")
    public Result<List<Map<String, Object>>> availableCourses(
            @RequestParam String studentId,
            @RequestParam(required = false) String semesterId,
            Authentication authentication) {
        studentId = studentAccessGuard.currentStudentId(authentication, studentId);
        return Result.success(selectionService.getAvailableCourses(studentId, semesterId));
    }

    // [迭代补充] 管理员分页查询选课记录
    @GetMapping("/page")
    public Result<IPage<StudentCourseSelection>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String status) {
        Page<StudentCourseSelection> page = new Page<>(current, size);
        LambdaQueryWrapper<StudentCourseSelection> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.eq(StudentCourseSelection::getStudentId, studentId);
        }
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(StudentCourseSelection::getSemesterId, semesterId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(StudentCourseSelection::getStatus, status);
        }
        wrapper.orderByDesc(StudentCourseSelection::getCreatedAt);
        return Result.success(selectionService.page(page, wrapper));
    }
}
