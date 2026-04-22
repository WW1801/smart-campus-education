package com.campus.education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.Grade;

import java.util.List;
import java.util.Map;

public interface GradeService extends IService<Grade> {
    void submitGrade(Grade grade);
    void batchSubmitGrades(List<Grade> grades);
    void approveGrade(String gradeId);
    void rejectGrade(String gradeId, String reason);
    Map<String, Object> calculateGpa(String studentId, String semesterId);
    Map<String, Object> getStatistics(String semesterId, String courseId, String classId);
}
