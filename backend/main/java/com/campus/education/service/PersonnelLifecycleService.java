package com.campus.education.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.common.BusinessException;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.Grade;
import com.campus.education.entity.Schedule;
import com.campus.education.entity.StudentCourseSelection;
import com.campus.education.entity.User;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.GradeMapper;
import com.campus.education.mapper.ScheduleMapper;
import com.campus.education.mapper.StudentCourseSelectionMapper;
import com.campus.education.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 人员状态与登录账号的生命周期统一入口：人员不可用时，其账号也不得继续用于登录或业务操作。
 */
@Service
public class PersonnelLifecycleService {
    private final UserService userService;
    private final AttendanceMapper attendanceMapper;
    private final GradeMapper gradeMapper;
    private final ScheduleMapper scheduleMapper;
    private final StudentCourseSelectionMapper selectionMapper;

    public PersonnelLifecycleService(UserService userService, AttendanceMapper attendanceMapper,
                                    GradeMapper gradeMapper, ScheduleMapper scheduleMapper,
                                    StudentCourseSelectionMapper selectionMapper) {
        this.userService = userService;
        this.attendanceMapper = attendanceMapper;
        this.gradeMapper = gradeMapper;
        this.scheduleMapper = scheduleMapper;
        this.selectionMapper = selectionMapper;
    }

    public void verifyStudentCanDelete(String studentId) {
        boolean hasAttendance = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, studentId)) > 0;
        boolean hasGrade = gradeMapper.selectCount(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, studentId)) > 0;
        boolean hasSelection = selectionMapper.selectCount(new LambdaQueryWrapper<StudentCourseSelection>()
                .eq(StudentCourseSelection::getStudentId, studentId)) > 0;
        boolean hasUser = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getRelatedId, studentId).eq(User::getRoleId, "5")) > 0;
        if (hasAttendance || hasGrade || hasSelection || hasUser) {
            throw new BusinessException("该学生存在登录账号、考勤、成绩或选课历史，不能删除。请改为停用或退学，以保留历史业务数据。");
        }
    }

    public void verifyTeacherCanDelete(String teacherId) {
        boolean hasSchedule = scheduleMapper.selectCount(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getTeacherId, teacherId)) > 0;
        boolean hasGrade = gradeMapper.selectCount(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getTeacherId, teacherId)) > 0;
        boolean hasUser = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getRelatedId, teacherId).eq(User::getRoleId, "4")) > 0;
        if (hasSchedule || hasGrade || hasUser) {
            throw new BusinessException("该教师存在登录账号、排课或成绩历史，不能删除。请改为停用或离职，以保留历史业务数据。");
        }
    }
}
