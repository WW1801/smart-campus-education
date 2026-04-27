package com.campus.education.service;

/**
 * 课程名单服务接口，定义课程名单相关业务能力。
 */

import com.campus.education.entity.Student;

import java.util.List;

public interface CourseRosterService {
    // 查询在籍学生列表
    List<Student> listActiveStudents(String courseId, String semesterId, String teacherId);
}
