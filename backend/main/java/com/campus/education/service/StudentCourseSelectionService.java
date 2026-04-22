package com.campus.education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.StudentCourseSelection;

import java.util.List;
import java.util.Map;

public interface StudentCourseSelectionService extends IService<StudentCourseSelection> {

    StudentCourseSelection selectCourse(String studentId, String scheduleId);

    void dropCourse(String selectionId);

    List<StudentCourseSelection> getMyCourses(String studentId, String semesterId);

    List<Map<String, Object>> getMySchedule(String studentId, String semesterId);

    List<Map<String, Object>> getAvailableCourses(String studentId, String semesterId);
}
