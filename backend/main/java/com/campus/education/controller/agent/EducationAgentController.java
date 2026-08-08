package com.campus.education.controller.agent;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.education.common.Result;
import com.campus.education.dto.agent.AcademicWarningDetailDTO;
import com.campus.education.dto.agent.AcademicWarningListDTO;
import com.campus.education.dto.agent.AcademicWarningProcessUpdateDTO;
import com.campus.education.dto.agent.AcademicWarningRecordDTO;
import com.campus.education.dto.agent.AcademicWarningSnapshotRequestDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionRequestDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionResponseDTO;
import com.campus.education.dto.agent.EducationMetricsOverviewDTO;
import com.campus.education.dto.agent.StudentGradeTrendDTO;
import com.campus.education.service.EducationAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 智能教务数据分析 Agent 接口。
 * 所有接口只调用固定 Service 指标方法，不提供自由 Text2SQL 能力。
 */
@RestController
@RequestMapping("/agent")
public class EducationAgentController {

    @Autowired
    private EducationAgentService educationAgentService;

    /**
     * 查询学业预警学生列表。
     * 请求参数：semesterId、departmentId、majorId、classId、keyword、level、page、pageSize。
     * 返回结果：分页返回学生风险等级、风险分、风险原因和命中规则数量。
     */
    @GetMapping("/warnings/students")
    public Result<IPage<AcademicWarningListDTO>> warnings(
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String majorId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // Controller 仅转发筛选和分页参数，风险规则、排序与分页均由 Service 处理。
        return Result.success(educationAgentService.pageAcademicWarnings(
                semesterId, departmentId, majorId, classId, keyword, level, page, pageSize));
    }

    /**
     * 查询单个学生学业预警详情。
     * 请求参数：studentId 路径参数，semesterId 可选学期参数。
     * 返回结果：风险等级、风险分、命中规则、风险原因和干预建议。
     */
    @GetMapping("/warnings/students/{studentId}")
    public Result<AcademicWarningDetailDTO> warningDetail(
            @PathVariable String studentId,
            @RequestParam(required = false) String semesterId) {
        return Result.success(educationAgentService.getAcademicWarningDetail(studentId, semesterId));
    }

    /**
     * 成绩趋势由固定 Service 汇总；不接收 SQL，也不改变既有 /agent/** 权限。
     */
    @GetMapping("/warnings/students/{studentId}/grade-trend")
    public Result<StudentGradeTrendDTO> warningGradeTrend(@PathVariable String studentId) {
        return Result.success(educationAgentService.getAcademicWarningGradeTrend(studentId));
    }

    /** 保存指定学生当前学期的规则计算快照，返回新增预警历史记录。 */
    @PostMapping("/warnings/students/{studentId}/records")
    public Result<AcademicWarningRecordDTO> saveWarningRecord(
            @PathVariable String studentId,
            @RequestParam(required = false) String semesterId) {
        return Result.success("学业预警记录已保存",
                educationAgentService.saveAcademicWarningRecord(studentId, semesterId));
    }

    /**
     * 查询指定学生的预警历史。
     * 请求参数：studentId 为学生内部 ID；semesterId、processStatus、riskLevel 为可选筛选；page、pageSize 为分页参数。
     * 返回结果：统一 Result 的 data 为分页对象，records 为历史记录数组，空历史返回空数组。
     */
    @GetMapping("/warnings/students/{studentId}/records")
    public Result<IPage<AcademicWarningRecordDTO>> warningHistory(
            @PathVariable String studentId,
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String processStatus,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(educationAgentService.pageAcademicWarningHistory(
                studentId, semesterId, processStatus, riskLevel, page, pageSize));
    }

    /** 管理员更新预警处理状态与意见，返回更新后的历史记录。 */
    @PutMapping("/warnings/records/{recordId}/process")
    public Result<AcademicWarningRecordDTO> updateWarningProcess(
            @PathVariable String recordId,
            @RequestBody AcademicWarningProcessUpdateDTO request,
            Authentication authentication) {
        String processedBy = authentication == null ? null : authentication.getName();
        return Result.success("学业预警处理状态已更新",
                educationAgentService.updateAcademicWarningProcess(recordId, request, processedBy));
    }

    /** 按筛选范围批量生成预警快照，返回本次保存的历史记录列表。 */
    @PostMapping("/warnings/records/snapshot")
    public Result<List<AcademicWarningRecordDTO>> saveWarningSnapshot(
            @RequestBody(required = false) AcademicWarningSnapshotRequestDTO request) {
        AcademicWarningSnapshotRequestDTO body = request == null ? new AcademicWarningSnapshotRequestDTO() : request;
        return Result.success("学业预警快照已保存", educationAgentService.saveAcademicWarningSnapshot(
                body.getSemesterId(), body.getDepartmentId(), body.getMajorId(),
                body.getClassId(), body.getKeyword(), body.getLevel()));
    }

    @GetMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam(required = false) String semesterId,
                                               @RequestParam(required = false) String departmentId,
                                               @RequestParam(required = false) String majorId,
                                               @RequestParam(required = false) String classId) {
        return Result.success(educationAgentService.getDashboard(semesterId, departmentId, majorId, classId));
    }

    @GetMapping("/analysis")
    public Result<Map<String, Object>> analysis(@RequestParam(required = false) String semesterId,
                                                @RequestParam(required = false) String departmentId,
                                                @RequestParam(required = false) String majorId,
                                                @RequestParam(required = false) String classId) {
        return Result.success(educationAgentService.getDashboard(semesterId, departmentId, majorId, classId));
    }

    /** 管理概览页的专业维度数据；不接受自由分组字段，避免产生任意查询。 */
    @GetMapping("/analysis/major-summary")
    public Result<List<Map<String, Object>>> majorSummary(
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String majorId,
            @RequestParam(required = false) String classId) {
        return Result.success(educationAgentService.getMajorStudentSummary(
                semesterId, departmentId, majorId, classId));
    }

    /** 教务概览：调用 EducationAgentService#getOverview。 */
    @GetMapping("/metrics/overview")
    public Result<EducationMetricsOverviewDTO> overview(@RequestParam(required = false) String semesterId,
                                                        @RequestParam(required = false) String departmentId,
                                                        @RequestParam(required = false) String majorId,
                                                        @RequestParam(required = false) String classId) {
        return Result.success(educationAgentService.getOverview(semesterId, departmentId, majorId, classId));
    }

    /** 成绩分析：调用 EducationAgentService#getGradeMetrics。 */
    @GetMapping("/metrics/grade")
    public Result<Map<String, Object>> grade(@RequestParam(required = false) String semesterId,
                                             @RequestParam(required = false) String departmentId,
                                             @RequestParam(required = false) String majorId,
                                             @RequestParam(required = false) String classId) {
        return Result.success(educationAgentService.getGradeMetrics(semesterId, departmentId, majorId, classId));
    }

    /** 考勤分析：调用 EducationAgentService#getAttendanceMetrics。 */
    @GetMapping("/metrics/attendance")
    public Result<Map<String, Object>> attendance(@RequestParam(required = false) String semesterId,
                                                  @RequestParam(required = false) String departmentId,
                                                  @RequestParam(required = false) String majorId,
                                                  @RequestParam(required = false) String classId) {
        return Result.success(educationAgentService.getAttendanceMetrics(semesterId, departmentId, majorId, classId));
    }

    /** 课程负载分析：调用 EducationAgentService#getCourseLoadMetrics。 */
    @GetMapping("/metrics/course")
    public Result<Map<String, Object>> courseLoad(@RequestParam(required = false) String semesterId,
                                                  @RequestParam(required = false) String departmentId,
                                                  @RequestParam(required = false) String majorId,
                                                  @RequestParam(required = false) String classId) {
        return Result.success(educationAgentService.getCourseLoadMetrics(semesterId, departmentId, majorId, classId));
    }

    /**
     * 规则版问数入口。后续接入大模型时保持该接口和 DTO 不变，
     * 只将关键词意图识别替换为模型意图识别，再调用相同的固定指标方法。
     */
    @PostMapping("/chat")
    public Result<EducationAnalysisQuestionResponseDTO> chat(
            @RequestBody EducationAnalysisQuestionRequestDTO request) {
        return Result.success(educationAgentService.answerQuestion(request));
    }

    /** 返回受控 Agent 能力和回退方式，不暴露密钥或自由 SQL 能力。 */
    @GetMapping("/capabilities")
    public Result<Map<String, Object>> capabilities() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "hybrid_rule_based");
        result.put("llmProvider", "deepseek");
        result.put("supportsTextToSql", false);
        result.put("fallback", "keyword_matching");
        Map<String, String> fixedServices = new LinkedHashMap<>();
        fixedServices.put("academic_risk", "EducationAgentService#getAcademicRiskAnalysis");
        fixedServices.put("grade_trend", "EducationAgentService#getStudentGradeTrend");
        fixedServices.put("failed_course", "EducationAgentService#getFailedCourseAnalysis");
        fixedServices.put("attendance_abnormal", "EducationAgentService#getAttendanceAbnormalAnalysis");
        fixedServices.put("course_load", "EducationAgentService#getCourseLoadMetrics");
        fixedServices.put("student_comparison", "EducationAgentService#getStudentComparison");
        result.put("fixedServices", fixedServices);
        return Result.success(result);
    }
}
