package com.campus.education.service;

/**
 * 排课服务接口，定义排课相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.CourseSchedule;

import java.util.List;
import java.util.Map;

public interface CourseScheduleService extends IService<CourseSchedule> {

    // 检查冲突
    List<Map<String, Object>> checkConflict(CourseSchedule schedule);

    // 保存并执行冲突检查
    CourseSchedule saveWithConflictCheck(CourseSchedule schedule);
}
