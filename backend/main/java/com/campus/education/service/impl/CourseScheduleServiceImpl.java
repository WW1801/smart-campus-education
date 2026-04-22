package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.entity.Course;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.Teacher;
import com.campus.education.mapper.CourseScheduleMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.CourseScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule>
        implements CourseScheduleService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Override
    public List<Map<String, Object>> checkConflict(CourseSchedule schedule) {
        List<Map<String, Object>> conflicts = new ArrayList<>();

        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getSemesterId, schedule.getSemesterId());
        wrapper.eq(CourseSchedule::getDayOfWeek, schedule.getDayOfWeek());
        if (schedule.getScheduleId() != null) {
            wrapper.ne(CourseSchedule::getScheduleId, schedule.getScheduleId());
        }
        List<CourseSchedule> sameTimeSchedules = this.list(wrapper);

        for (CourseSchedule existing : sameTimeSchedules) {
            // [迭代补充] 时间段重叠判定：start1 <= end2 AND start2 <= end1
            if (existing.getStartPeriod() <= schedule.getEndPeriod()
                    && schedule.getStartPeriod() <= existing.getEndPeriod()) {

                // [迭代补充] P0：教师时间冲突（最高优先级）
                if (existing.getTeacherId().equals(schedule.getTeacherId())) {
                    conflicts.add(buildConflictDetail("teacher", "P0", existing,
                            "教师时间冲突：该教师在同一时段已有排课"));
                }

                // [迭代补充] P2：教室时间冲突
                if (existing.getClassroomId().equals(schedule.getClassroomId())) {
                    conflicts.add(buildConflictDetail("classroom", "P2", existing,
                            "教室时间冲突：该教室在同一时段已被占用"));
                }

                // [迭代补充] P3：班级时间冲突
                if (schedule.getClassId() != null && schedule.getClassId().equals(existing.getClassId())) {
                    conflicts.add(buildConflictDetail("class", "P3", existing,
                            "班级时间冲突：该班级在同一时段已有排课"));
                }
            }
        }

        // [迭代补充] P1：教室容量检查（非时间冲突，但需校验）
        if (schedule.getClassroomId() != null) {
            // 容量校验在调用方处理，此处仅记录日志
            log.info("排课冲突检测完成：scheduleId={}, 检测到{}个冲突",
                    schedule.getScheduleId(), conflicts.size());
        }

        return conflicts;
    }

    @Override
    public CourseSchedule saveWithConflictCheck(CourseSchedule schedule) {
        List<Map<String, Object>> conflicts = checkConflict(schedule);

        // [迭代补充] P0级冲突不可调和，直接拒绝
        boolean hasP0 = conflicts.stream().anyMatch(c -> "P0".equals(c.get("priority")));
        if (hasP0) {
            String messages = String.join("; ", conflicts.stream()
                    .map(c -> (String) c.get("message"))
                    .toArray(String[]::new));
            log.warn("排课被拒绝（P0冲突）：{}", messages);
            throw new BusinessException("排课冲突（不可调和）：" + messages);
        }

        // [迭代补充] P2/P3级冲突记录日志但允许保存（可换教室/调时段解决）
        if (!conflicts.isEmpty()) {
            for (Map<String, Object> conflict : conflicts) {
                log.warn("排课冲突警告：priority={}, type={}, message={}",
                        conflict.get("priority"), conflict.get("type"), conflict.get("message"));
            }
        }

        if (schedule.getScheduleId() != null && !schedule.getScheduleId().trim().isEmpty()) {
            this.updateById(schedule);
        } else {
            this.save(schedule);
        }
        return schedule;
    }

    private Map<String, Object> buildConflictDetail(String type, String priority, CourseSchedule existing, String message) {
        Course course = courseMapper.selectById(existing.getCourseId());
        Teacher teacher = teacherMapper.selectById(existing.getTeacherId());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("type", type);
        detail.put("priority", priority);
        detail.put("message", message);
        detail.put("conflictScheduleId", existing.getScheduleId());
        detail.put("conflictCourseId", existing.getCourseId());
        detail.put("conflictCourseName", course != null ? course.getName() : "");
        detail.put("conflictTeacherName", teacher != null ? teacher.getName() : "");
        detail.put("conflictDayOfWeek", existing.getDayOfWeek());
        detail.put("conflictStartPeriod", existing.getStartPeriod());
        detail.put("conflictEndPeriod", existing.getEndPeriod());
        return detail;
    }
}
