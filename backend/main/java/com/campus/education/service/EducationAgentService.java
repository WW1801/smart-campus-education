package com.campus.education.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.education.dto.agent.AcademicWarningDetailDTO;
import com.campus.education.dto.agent.AcademicWarningListDTO;
import com.campus.education.dto.agent.AcademicWarningProcessUpdateDTO;
import com.campus.education.dto.agent.AcademicWarningRecordDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionRequestDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionResponseDTO;
import com.campus.education.dto.agent.EducationMetricsOverviewDTO;
import com.campus.education.dto.agent.StudentGradeTrendDTO;

import java.util.List;
import java.util.Map;

/**
 * 智能教务数据分析 Agent 的固定指标服务。
 * 第一版仅提供白名单指标查询；后续大模型可复用本接口的方法，不直接生成 SQL。
 */
public interface EducationAgentService {

    IPage<AcademicWarningListDTO> pageAcademicWarnings(String semesterId, String departmentId, String majorId,
                                                       String classId, String keyword, String level,
                                                       Integer page, Integer pageSize);

    AcademicWarningDetailDTO getAcademicWarningDetail(String studentId, String semesterId);

    AcademicWarningRecordDTO saveAcademicWarningRecord(String studentId, String semesterId);

    IPage<AcademicWarningRecordDTO> pageAcademicWarningHistory(String studentId, String semesterId,
                                                               String processStatus, String riskLevel,
                                                               Integer page, Integer pageSize);

    AcademicWarningRecordDTO updateAcademicWarningProcess(String recordId, AcademicWarningProcessUpdateDTO request,
                                                          String processedBy);

    List<AcademicWarningRecordDTO> saveAcademicWarningSnapshot(String semesterId, String departmentId, String majorId,
                                                               String classId, String keyword, String level);

    EducationMetricsOverviewDTO getOverview(String semesterId, String departmentId, String majorId, String classId);

    Map<String, Object> getGradeMetrics(String semesterId, String departmentId, String majorId, String classId);

    Map<String, Object> getAttendanceMetrics(String semesterId, String departmentId, String majorId, String classId);

    Map<String, Object> getCourseLoadMetrics(String semesterId, String departmentId, String majorId, String classId);

    /**
     * 固定专业汇总：供管理概览页展示学生规模及预警率。
     * 此方法只使用后端预定义的查询与预警规则，模型不能传入 SQL。
     */
    List<Map<String, Object>> getMajorStudentSummary(String semesterId, String departmentId, String majorId,
                                                     String classId);

    /** Fixed tool: evaluates academic-warning rules; it never accepts SQL. */
    Map<String, Object> getAcademicRiskAnalysis(String semesterId, String departmentId, String majorId, String classId);

    /** Fixed tool: returns one student's approved-grade trend grouped by semester. */
    Map<String, Object> getStudentGradeTrend(String studentId);

    /** Fixed academic-warning tool: calculates semester grade trend and risk change for one verified student. */
    StudentGradeTrendDTO getAcademicWarningGradeTrend(String studentId);

    /** Fixed tool: aggregates failed approved grades by course under the requested scope. */
    Map<String, Object> getFailedCourseAnalysis(String semesterId, String departmentId, String majorId, String classId,
                                                String studentId);

    /** Fixed tool: aggregates abnormal attendance records and affected students. */
    Map<String, Object> getAttendanceAbnormalAnalysis(String semesterId, String departmentId, String majorId,
                                                       String classId, String studentId);

    /** Fixed tool: compares two explicitly supplied students using fixed grade/attendance queries. */
    Map<String, Object> getStudentComparison(String studentId, String compareStudentId, String semesterId);

    Map<String, Object> getDashboard(String semesterId, String departmentId, String majorId, String classId);

    EducationAnalysisQuestionResponseDTO answerQuestion(EducationAnalysisQuestionRequestDTO request);
}
