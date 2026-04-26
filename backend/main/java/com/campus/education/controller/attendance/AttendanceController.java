package com.campus.education.controller.attendance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.common.StudentAccessGuard;
import com.campus.education.entity.Attendance;
import com.campus.education.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private StudentAccessGuard studentAccessGuard;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer limit,
                                            @RequestParam(required = false) String semesterId,
                                            @RequestParam(required = false) String courseId,
                                            @RequestParam(required = false) String studentId,
                                            @RequestParam(required = false) String classId,
                                            @RequestParam(required = false) String date,
                                            @RequestParam(required = false) String status,
                                            Authentication authentication) {
        studentId = studentAccessGuard.resolveStudentFilter(authentication, studentId);
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Attendance::getSemesterId, semesterId);
        }
        if (courseId != null && !courseId.trim().isEmpty()) {
            wrapper.eq(Attendance::getCourseId, courseId);
        }
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.eq(Attendance::getStudentId, studentId);
        }
        if (classId != null && !classId.trim().isEmpty()) {
            wrapper.eq(Attendance::getStudentId, classId);
        }
        if (date != null && !date.trim().isEmpty()) {
            wrapper.eq(Attendance::getDate, date);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Attendance::getStatus, status);
        }
        wrapper.orderByDesc(Attendance::getDate);
        Page<Attendance> pageParam = new Page<>(page, limit);
        IPage<Attendance> result = attendanceService.page(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success("查询成功", data);
    }

    @PostMapping
    public Result<Void> add(@RequestBody Attendance attendance) {
        attendanceService.save(attendance);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Attendance attendance) {
        attendanceService.updateById(attendance);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        attendanceService.removeById(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/batch-save")
    public Result<Void> batchSave(@RequestBody Map<String, Object> params) {
        return Result.success("批量保存成功", null);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(required = false) String semesterId,
                                                  @RequestParam(required = false) String courseId,
                                                  @RequestParam(required = false) String classId) {
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Attendance::getSemesterId, semesterId);
        }
        if (courseId != null && !courseId.trim().isEmpty()) {
            wrapper.eq(Attendance::getCourseId, courseId);
        }

        long totalCount = attendanceService.count(wrapper);
        long presentCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "present"));
        long lateCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "late"));
        long absentCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "absent"));
        long leaveCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "leave"));
        long earlyCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "early"));

        Map<String, Object> data = new HashMap<>();
        data.put("totalClasses", totalCount);
        data.put("presentCount", presentCount);
        data.put("lateCount", lateCount);
        data.put("earlyCount", earlyCount);
        data.put("absentCount", absentCount);
        data.put("leaveCount", leaveCount);
        data.put("attendanceRate", totalCount > 0 ? String.format("%.1f%%", (double) presentCount / totalCount * 100) : "0%");

        java.util.List<Map<String, Object>> trend = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate queryDate = today.minusDays(i);
            long dayTotal = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                    .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                    .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                    .eq(Attendance::getDate, queryDate));
            long dayPresent = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                    .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                    .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                    .eq(Attendance::getDate, queryDate)
                    .eq(Attendance::getStatus, "present"));
            Map<String, Object> dayData = new HashMap<>();
            java.time.DayOfWeek dow = queryDate.getDayOfWeek();
            dayData.put("day", dayNames[dow.getValue() - 1]);
            dayData.put("rate", dayTotal > 0 ? Math.round((double) dayPresent / dayTotal * 100) : 0);
            trend.add(dayData);
        }
        data.put("trend", trend);

        return Result.success("查询成功", data);
    }
}
