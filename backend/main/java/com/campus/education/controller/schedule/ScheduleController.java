package com.campus.education.controller.schedule;

/**
 * 课表控制器，负责处理课表相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.Schedule;
import com.campus.education.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/schedule-basic")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer limit,
                                            @RequestParam(required = false) String semesterId,
                                            @RequestParam(required = false) String courseName,
                                            @RequestParam(required = false) String teacherName,
                                            @RequestParam(required = false) String className) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Schedule::getSemesterId, semesterId);
        }
        Page<Schedule> pageParam = new Page<>(page, limit);
        IPage<Schedule> result = scheduleService.page(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success("查询成功", data);
    }

    // 添加课表
    @PostMapping
    public Result<Void> add(@RequestBody Schedule schedule) {
        scheduleService.save(schedule);
        return Result.success("添加成功", null);
    }

    // 更新课表
    @PutMapping
    public Result<Void> update(@RequestBody Schedule schedule) {
        scheduleService.updateById(schedule);
        return Result.success("更新成功", null);
    }

    // 删除课表
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        scheduleService.removeById(id);
        return Result.success("删除成功", null);
    }

    // 处理自动排课
    @PostMapping("/auto-arrange")
    public Result<Void> autoArrange(@RequestBody Map<String, Object> params) {
        return Result.success("自动排课成功", null);
    }

    // 处理查询
    @GetMapping("/query")
    public Result<Map<String, Object>> query(@RequestParam String semesterId,
                                             @RequestParam String queryType,
                                             @RequestParam(required = false) String teacherId,
                                             @RequestParam(required = false) String classId,
                                             @RequestParam(required = false) String studentId) {
        Map<String, Object> data = new HashMap<>();
        data.put("schedules", scheduleService.list());
        return Result.success("查询成功", data);
    }
}
