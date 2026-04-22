package com.campus.education.controller.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedule")
public class CourseScheduleController {

    @Autowired
    private CourseScheduleService courseScheduleService;

    @GetMapping("/page")
    public Result<IPage<CourseSchedule>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String classroomId) {
        Page<CourseSchedule> page = new Page<>(current, size);
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(CourseSchedule::getSemesterId, semesterId);
        }
        if (teacherId != null && !teacherId.trim().isEmpty()) {
            wrapper.eq(CourseSchedule::getTeacherId, teacherId);
        }
        if (classId != null && !classId.trim().isEmpty()) {
            wrapper.eq(CourseSchedule::getClassId, classId);
        }
        if (classroomId != null && !classroomId.trim().isEmpty()) {
            wrapper.eq(CourseSchedule::getClassroomId, classroomId);
        }
        wrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
        return Result.success(courseScheduleService.page(page, wrapper));
    }

    @GetMapping("/list")
    public Result<List<CourseSchedule>> list(
            @RequestParam(required = false) String semesterId) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(CourseSchedule::getSemesterId, semesterId);
        }
        wrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
        return Result.success(courseScheduleService.list(wrapper));
    }

    // [迭代补充] 排课冲突检测接口（不保存，仅返回冲突信息）
    @PostMapping("/check-conflict")
    public Result<List<Map<String, Object>>> checkConflict(@RequestBody CourseSchedule schedule) {
        List<Map<String, Object>> conflicts = courseScheduleService.checkConflict(schedule);
        return Result.success(conflicts);
    }

    // [迭代补充] 新增排课（含冲突检测，P0冲突拒绝保存）
    @PostMapping
    public Result<CourseSchedule> add(@RequestBody CourseSchedule schedule) {
        CourseSchedule saved = courseScheduleService.saveWithConflictCheck(schedule);
        return Result.success("排课成功", saved);
    }

    // [迭代补充] 修改排课（含冲突检测）
    @PutMapping
    public Result<CourseSchedule> update(@RequestBody CourseSchedule schedule) {
        CourseSchedule saved = courseScheduleService.saveWithConflictCheck(schedule);
        return Result.success("修改成功", saved);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        courseScheduleService.removeById(id);
        return Result.success("删除成功", null);
    }

    // [迭代补充] 按教师查询课表
    @GetMapping("/query/by-teacher")
    public Result<List<CourseSchedule>> queryByTeacher(
            @RequestParam String teacherId,
            @RequestParam String semesterId) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getTeacherId, teacherId);
        wrapper.eq(CourseSchedule::getSemesterId, semesterId);
        wrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
        return Result.success(courseScheduleService.list(wrapper));
    }

    // [迭代补充] 按班级查询课表
    @GetMapping("/query/by-class")
    public Result<List<CourseSchedule>> queryByClass(
            @RequestParam String classId,
            @RequestParam String semesterId) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getClassId, classId);
        wrapper.eq(CourseSchedule::getSemesterId, semesterId);
        wrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
        return Result.success(courseScheduleService.list(wrapper));
    }

    // [迭代补充] 按教室查询课表
    @GetMapping("/query/by-classroom")
    public Result<List<CourseSchedule>> queryByClassroom(
            @RequestParam String classroomId,
            @RequestParam String semesterId) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getClassroomId, classroomId);
        wrapper.eq(CourseSchedule::getSemesterId, semesterId);
        wrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
        return Result.success(courseScheduleService.list(wrapper));
    }
}
