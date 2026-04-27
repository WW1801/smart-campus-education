package com.campus.education.service;

/**
 * 学生选课服务接口，定义学生选课相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.StudentCourseSelection;

import java.util.List;
import java.util.Map;

public interface StudentCourseSelectionService extends IService<StudentCourseSelection> {

    // 选择课程
    StudentCourseSelection selectCourse(String studentId, String scheduleId);

    // 处理退选课程
    void dropCourse(String selectionId);

    // 获取我的课程
    List<StudentCourseSelection> getMyCourses(String studentId, String semesterId);

    // 获取我的课表
    List<Map<String, Object>> getMySchedule(String studentId, String semesterId);

    // 获取学生课表
    List<Map<String, Object>> getStudentTimetable(String studentId, String semesterId);

    // 获取可选课程
    List<Map<String, Object>> getAvailableCourses(String studentId, String semesterId);
}
