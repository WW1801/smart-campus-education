package com.campus.education.controller.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.Student;
import com.campus.education.entity.StudentCourseSelection;
import com.campus.education.service.CourseScheduleService;
import com.campus.education.service.StudentCourseSelectionService;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/schedule")
public class CourseScheduleController {

    @Autowired
    private CourseScheduleService courseScheduleService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentCourseSelectionService studentCourseSelectionService;

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
    public Result<List<CourseSchedule>> list(@RequestParam(required = false) String semesterId) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(CourseSchedule::getSemesterId, semesterId);
        }
        wrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
        return Result.success(courseScheduleService.list(wrapper));
    }

    @PostMapping("/check-conflict")
    public Result<List<Map<String, Object>>> checkConflict(@RequestBody CourseSchedule schedule) {
        return Result.success(courseScheduleService.checkConflict(schedule));
    }

    @PostMapping
    public Result<CourseSchedule> add(@RequestBody CourseSchedule schedule) {
        return Result.success("排课成功", courseScheduleService.saveWithConflictCheck(schedule));
    }

    @PutMapping
    public Result<CourseSchedule> update(@RequestBody CourseSchedule schedule) {
        return Result.success("修改成功", courseScheduleService.saveWithConflictCheck(schedule));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        courseScheduleService.removeById(id);
        return Result.success("删除成功", null);
    }

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

    @GetMapping("/query/by-student")
    public Result<List<CourseSchedule>> queryByStudent(
            @RequestParam String studentId,
            @RequestParam String semesterId) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            return Result.badRequest("学生不存在");
        }

        Map<String, CourseSchedule> mergedSchedules = new LinkedHashMap<>();

        if (student.getClassId() != null && !student.getClassId().trim().isEmpty()) {
            LambdaQueryWrapper<CourseSchedule> classWrapper = new LambdaQueryWrapper<>();
            classWrapper.eq(CourseSchedule::getClassId, student.getClassId());
            classWrapper.eq(CourseSchedule::getSemesterId, semesterId);
            classWrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
            courseScheduleService.list(classWrapper).forEach(item -> mergedSchedules.put(item.getScheduleId(), item));
        }

        List<String> selectedScheduleIds = studentCourseSelectionService.getMyCourses(studentId, semesterId).stream()
                .map(StudentCourseSelection::getScheduleId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (!selectedScheduleIds.isEmpty()) {
            courseScheduleService.listByIds(selectedScheduleIds).stream()
                    .filter(item -> semesterId.equals(item.getSemesterId()))
                    .forEach(item -> mergedSchedules.put(item.getScheduleId(), item));
        }

        List<CourseSchedule> schedules = new ArrayList<>(mergedSchedules.values());
        schedules.sort(Comparator.comparing(CourseSchedule::getDayOfWeek)
                .thenComparing(CourseSchedule::getStartPeriod)
                .thenComparing(CourseSchedule::getEndPeriod));
        return Result.success(schedules);
    }

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
