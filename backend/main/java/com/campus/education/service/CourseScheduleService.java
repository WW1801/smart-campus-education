package com.campus.education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.CourseSchedule;

import java.util.List;
import java.util.Map;

public interface CourseScheduleService extends IService<CourseSchedule> {

    List<Map<String, Object>> checkConflict(CourseSchedule schedule);

    CourseSchedule saveWithConflictCheck(CourseSchedule schedule);
}
