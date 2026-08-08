package com.campus.education.service;

/**
 * 成绩服务接口，定义成绩相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.Grade;

import java.util.List;
import java.util.Map;

public interface GradeService extends IService<Grade> {
    // 处理提交成绩
    void submitGrade(Grade grade);
    // 批量提交成绩
    void batchSubmitGrades(List<Grade> grades);
    // 处理通过成绩
    void approveGrade(String gradeId);
    // 处理驳回成绩
    void rejectGrade(String gradeId, String reason);
    // 计算绩点
    Map<String, Object> calculateGpa(String studentId, String semesterId);
    // 获取统计
    Map<String, Object> getStatistics(String semesterId, String courseId, String classId);

    /**
     * 学业预警的固定成绩数据源：只返回指定学生已审核的成绩。
     * Agent 只能消费该方法的返回值，不接收或拼接 SQL。
     */
    List<Grade> listApprovedGradesByStudent(String studentId);
}
