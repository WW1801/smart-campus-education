package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.education.common.BusinessException;
import com.campus.education.dto.GraduationAuditVO;
import com.campus.education.dto.agent.AcademicWarningDetailDTO;
import com.campus.education.dto.agent.AcademicWarningListDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionRequestDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionResponseDTO;
import com.campus.education.dto.agent.StudentGradeTrendDTO;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.Grade;
import com.campus.education.entity.GraduationAudit;
import com.campus.education.entity.Student;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.ClassMapper;
import com.campus.education.mapper.GradeMapper;
import com.campus.education.mapper.MajorMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.GraduationAuditService;
import com.campus.education.service.GradeService;
import com.campus.education.service.AttendanceService;
import com.campus.education.service.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EducationAgentServiceImplTest {

    private EducationAgentServiceImpl service;
    private StudentMapper studentMapper;
    private GradeMapper gradeMapper;
    private AttendanceMapper attendanceMapper;
    private GraduationAuditService graduationAuditService;
    private GradeService gradeService;
    private AttendanceService attendanceService;
    private MajorMapper majorMapper;
    private ClassMapper classMapper;

    @BeforeEach
    void setUp() {
        service = new EducationAgentServiceImpl();
        studentMapper = mock(StudentMapper.class);
        gradeMapper = mock(GradeMapper.class);
        attendanceMapper = mock(AttendanceMapper.class);
        graduationAuditService = mock(GraduationAuditService.class);
        gradeService = mock(GradeService.class);
        attendanceService = mock(AttendanceService.class);
        majorMapper = mock(MajorMapper.class);
        classMapper = mock(ClassMapper.class);

        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "gradeMapper", gradeMapper);
        ReflectionTestUtils.setField(service, "attendanceMapper", attendanceMapper);
        ReflectionTestUtils.setField(service, "graduationAuditService", graduationAuditService);
        ReflectionTestUtils.setField(service, "gradeService", gradeService);
        ReflectionTestUtils.setField(service, "attendanceService", attendanceService);
        ReflectionTestUtils.setField(service, "majorMapper", majorMapper);
        ReflectionTestUtils.setField(service, "classMapper", classMapper);
    }

    @Test
    void shouldUseHighestRiskWhenMultipleRulesAreTriggered() {
        Student student = buildStudent("S001", "张同学");
        when(studentMapper.selectById("S001")).thenReturn(student);
        when(gradeService.listApprovedGradesByStudent("S001"))
                .thenReturn(Arrays.asList(failedGrade("S001", "C001"), failedGrade("S001", "C002")));
        when(attendanceService.listByStudentForWarning("S001", "SEM001"))
                .thenReturn(Arrays.asList(absence("S001"), absence("S001"), absence("S001")));
        GraduationAuditVO audit = new GraduationAuditVO();
        audit.setStatus("rejected");
        when(graduationAuditService.getAuditDetail("S001")).thenReturn(audit);

        AcademicWarningDetailDTO detail = service.getAcademicWarningDetail("S001", "SEM001");

        assertEquals("high", detail.getRiskLevel());
        assertEquals(100, detail.getRiskScore());
        assertEquals(2, detail.getFailedCourseCount());
        assertEquals(3, detail.getAbsentCount());
        assertEquals(4, detail.getTriggeredRules().size());
        assertTrue(detail.getRiskReason().contains("毕业审核未通过，存在毕业风险"));
        assertTrue(detail.getRiskReason().contains("成绩、考勤、毕业审核风险同时存在"));
    }

    @Test
    void shouldReturnMediumRiskForOneFailedCourse() {
        Student student = buildStudent("S002", "李同学");
        when(studentMapper.selectById("S002")).thenReturn(student);
        when(gradeService.listApprovedGradesByStudent("S002"))
                .thenReturn(Collections.singletonList(failedGrade("S002", "C001")));
        when(attendanceService.listByStudentForWarning("S002", null))
                .thenReturn(Collections.<Attendance>emptyList());
        GraduationAuditVO audit = new GraduationAuditVO();
        audit.setStatus("unaudited");
        when(graduationAuditService.getAuditDetail("S002")).thenReturn(audit);

        AcademicWarningDetailDTO detail = service.getAcademicWarningDetail("S002", null);

        assertEquals("medium", detail.getRiskLevel());
        assertEquals(25, detail.getRiskScore());
        assertEquals(1, detail.getTriggeredRules().size());
    }

    @Test
    void shouldReturnMediumRiskForThreeAbsences() {
        Student student = buildStudent("S005", "陈同学");
        when(studentMapper.selectById("S005")).thenReturn(student);
        when(gradeService.listApprovedGradesByStudent("S005"))
                .thenReturn(Collections.<Grade>emptyList());
        when(attendanceService.listByStudentForWarning("S005", null))
                .thenReturn(Arrays.asList(absence("S005"), absence("S005"), absence("S005")));
        GraduationAuditVO audit = new GraduationAuditVO();
        audit.setStatus("approved");
        when(graduationAuditService.getAuditDetail("S005")).thenReturn(audit);

        AcademicWarningDetailDTO detail = service.getAcademicWarningDetail("S005", null);

        assertEquals("medium", detail.getRiskLevel());
        assertEquals(30, detail.getRiskScore());
        assertEquals(3, detail.getAbsentCount());
    }

    @Test
    void shouldReturnLowRiskWhenNoRiskDataExists() {
        Student student = buildStudent("S006", "赵同学");
        when(studentMapper.selectById("S006")).thenReturn(student);
        when(gradeService.listApprovedGradesByStudent("S006"))
                .thenReturn(Collections.<Grade>emptyList());
        when(attendanceService.listByStudentForWarning("S006", null))
                .thenReturn(Collections.<Attendance>emptyList());
        GraduationAuditVO audit = new GraduationAuditVO();
        audit.setStatus(null);
        when(graduationAuditService.getAuditDetail("S006")).thenReturn(audit);

        AcademicWarningDetailDTO detail = service.getAcademicWarningDetail("S006", null);

        assertEquals("low", detail.getRiskLevel());
        assertEquals(0, detail.getRiskScore());
        assertEquals("unaudited", detail.getGraduationAuditStatus());
        assertTrue(detail.getTriggeredRules().isEmpty());
    }

    @Test
    void shouldRejectWarningDetailWhenStudentDoesNotExist() {
        when(studentMapper.selectById("S404")).thenReturn(null);

        // 不存在的学生不继续查询成绩和考勤，由统一异常处理器转换为 Result 错误响应。
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getAcademicWarningDetail("S404", "SEM001"));

        assertEquals("学生不存在", exception.getMessage());
        verifyNoInteractions(gradeService, attendanceService);
    }

    @Test
    void shouldUseLatestGradePerCourseWhenCountingFailedCourses() {
        Student student = buildStudent("S007", "周同学");
        Grade olderFailedGrade = failedGrade("S007", "C001");
        olderFailedGrade.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        Grade newerPassedGrade = passedGrade("S007", "C001");
        newerPassedGrade.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));

        when(studentMapper.selectById("S007")).thenReturn(student);
        when(gradeService.listApprovedGradesByStudent("S007"))
                .thenReturn(Arrays.asList(olderFailedGrade, newerPassedGrade));
        when(attendanceService.listByStudentForWarning("S007", null))
                .thenReturn(Collections.<Attendance>emptyList());
        GraduationAuditVO audit = new GraduationAuditVO();
        audit.setStatus("approved");
        when(graduationAuditService.getAuditDetail("S007")).thenReturn(audit);

        AcademicWarningDetailDTO detail = service.getAcademicWarningDetail("S007", null);

        assertEquals(0, detail.getFailedCourseCount());
        assertEquals("low", detail.getRiskLevel());
        assertFalse(detail.getRiskReason().contains("GRADE_FAILED"));
    }

    @Test
    void shouldOnlyIncludeStudentsWhoTriggerWarningRules() {
        Student warningStudent = buildStudent("S003", "王同学");
        Student normalStudent = buildStudent("S004", "赵同学");
        when(studentMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Student>>any()))
                .thenReturn(Arrays.asList(warningStudent, normalStudent));
        when(gradeMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Grade>>any()))
                .thenReturn(Collections.singletonList(failedGrade("S003", "C001")));
        when(attendanceMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Attendance>>any()))
                .thenReturn(Collections.<Attendance>emptyList());
        when(graduationAuditService.list(ArgumentMatchers.<LambdaQueryWrapper<GraduationAudit>>any()))
                .thenReturn(Collections.<GraduationAudit>emptyList());

        IPage<AcademicWarningListDTO> page = service.pageAcademicWarnings(
                null, null, null, null, null, null, 1, 10);

        assertEquals(1, page.getTotal());
        assertEquals("S003", page.getRecords().get(0).getStudentId());
        assertEquals("medium", page.getRecords().get(0).getRiskLevel());
        assertEquals(page.getRecords().get(0).getStudentId(), page.getRecords().get(0).getStudentNo());
        assertEquals(page.getRecords().get(0).getRiskLevel(), page.getRecords().get(0).getLevel());
        assertEquals(page.getRecords().get(0).getRiskScore(), page.getRecords().get(0).getScore());
        assertEquals(page.getRecords().get(0).getRiskReason(), page.getRecords().get(0).getReason());
    }

    @Test
    void shouldFilterWarningListByStudentNumberNameRiskLevelAndSemesterThenPage() {
        Student highRiskStudent = buildStudent("S101", "张三");
        Student mediumRiskStudent = buildStudent("S102", "李四");
        Student normalStudent = buildStudent("S103", "王五");
        when(studentMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Student>>any()))
                .thenReturn(Arrays.asList(highRiskStudent, mediumRiskStudent, normalStudent));
        when(gradeMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Grade>>any()))
                .thenReturn(Arrays.asList(
                        failedGrade("S101", "C001"), failedGrade("S101", "C002"),
                        failedGrade("S102", "C003")));
        when(attendanceMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Attendance>>any()))
                .thenReturn(Collections.<Attendance>emptyList());
        when(graduationAuditService.list(ArgumentMatchers.<LambdaQueryWrapper<GraduationAudit>>any()))
                .thenReturn(Collections.<GraduationAudit>emptyList());

        // 固定查询先按学号/姓名过滤，再按规则计算后的等级过滤，最后才切分页。
        IPage<AcademicWarningListDTO> highRiskPage = service.pageAcademicWarnings(
                "SEM2026", null, null, null, "张三", "high", 1, 1);

        assertEquals(1, highRiskPage.getTotal());
        assertEquals("S101", highRiskPage.getRecords().get(0).getStudentId());
        assertEquals("high", highRiskPage.getRecords().get(0).getRiskLevel());
        assertFalse(highRiskPage.getRecords().get(0).getTriggeredRules().isEmpty());

        // semesterId 由 Service 接收并传入固定成绩/考勤查询；该单元测试不依赖 MyBatis 元数据解析。
        verify(gradeMapper).selectList(ArgumentMatchers.<LambdaQueryWrapper<Grade>>any());
    }

    @Test
    void shouldUseLlmForWhitelistedToolSelectionAndExplanation() {
        LlmClient llmClient = mock(LlmClient.class);
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        when(llmClient.recognizeIntent("请分析成绩")).thenReturn("attendance");
        when(llmClient.explain(ArgumentMatchers.anyString(), ArgumentMatchers.eq("attendance"),
                ArgumentMatchers.<java.util.Map<String, Object>>any(), ArgumentMatchers.any()))
                .thenReturn("模型仅基于后端指标完成考勤解读。");
        prepareEmptyQuestionMetrics();

        EducationAnalysisQuestionResponseDTO response = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question("请分析成绩").build());

        assertEquals("attendance", response.getIntentCode());
        assertEquals("模型仅基于后端指标完成考勤解读。", response.getAnswer());
    }

    @Test
    void shouldFallbackToKeywordMatchingWhenLlmReturnsUnsupportedIntent() {
        LlmClient llmClient = mock(LlmClient.class);
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        when(llmClient.recognizeIntent("请分析成绩")).thenReturn("SELECT * FROM grade");
        prepareEmptyQuestionMetrics();

        EducationAnalysisQuestionResponseDTO response = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question("请分析成绩").build());

        assertEquals("grade", response.getIntentCode());
        assertTrue(response.getAnswer().contains("没有已审核成绩记录"));
    }

    @Test
    void shouldFallbackToAcademicRiskKeywordWhenLlmReturnsIllegalIntent() {
        LlmClient llmClient = mock(LlmClient.class);
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        when(llmClient.recognizeIntent("请做学业风险分析")).thenReturn("DROP TABLE grade");
        prepareEmptyQuestionMetrics();
        when(graduationAuditService.list(ArgumentMatchers.<LambdaQueryWrapper<GraduationAudit>>any()))
                .thenReturn(Collections.<GraduationAudit>emptyList());

        EducationAnalysisQuestionResponseDTO response = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question("请做学业风险分析").build());

        assertEquals("academic_risk", response.getIntentCode());
        assertEquals("keyword", response.getIntentSource());
        assertTrue(response.getDetailRows().isEmpty());
    }

    @Test
    void shouldUseFixedGradeTrendToolForWhitelistedIntent() {
        LlmClient llmClient = mock(LlmClient.class);
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        when(llmClient.recognizeIntent("查看学生成绩趋势")).thenReturn("grade_trend");
        when(gradeMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Grade>>any()))
                .thenReturn(Collections.<Grade>emptyList());
        prepareEmptyQuestionMetrics();

        EducationAnalysisQuestionResponseDTO response = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question("查看学生成绩趋势").studentId("S001").build());

        assertEquals("grade_trend", response.getIntentCode());
        assertTrue(response.getAnswer().contains("无法确认学生身份"));
    }

    @Test
    void shouldCalculateSemesterMetricsTrendAndRiskChangeFromFixedGrades() {
        Student student = buildStudent("S008", "趋势同学");
        Grade firstPassed = passedGrade("S008", "C001");
        firstPassed.setSemesterId("2025-1");
        firstPassed.setTotalScore(80D);
        Grade firstFailed = failedGrade("S008", "C002");
        firstFailed.setSemesterId("2025-1");
        Grade secondPassed = passedGrade("S008", "C003");
        secondPassed.setSemesterId("2025-2");
        secondPassed.setTotalScore(85D);
        Grade secondPassedTwo = passedGrade("S008", "C004");
        secondPassedTwo.setSemesterId("2025-2");
        secondPassedTwo.setTotalScore(75D);
        when(studentMapper.selectById("S008")).thenReturn(student);
        when(gradeService.listApprovedGradesByStudent("S008"))
                .thenReturn(Arrays.asList(firstPassed, firstFailed, secondPassed, secondPassedTwo));

        StudentGradeTrendDTO trend = service.getAcademicWarningGradeTrend("S008");

        assertEquals(2, trend.getSemesters().size());
        assertEquals(65D, trend.getSemesters().get(0).getAverageScore());
        assertEquals(1, trend.getSemesters().get(0).getFailedCourseCount());
        assertEquals(2, trend.getSemesters().get(0).getCourseCount());
        assertEquals("上升", trend.getTrendDirection());
        assertEquals("风险下降", trend.getRiskChange());
        verify(gradeService).listApprovedGradesByStudent("S008");
    }

    @Test
    void shouldRejectSqlLikeLlmExplanationAndKeepFixedServiceAnswer() {
        LlmClient llmClient = mock(LlmClient.class);
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        when(llmClient.recognizeIntent("请分析成绩")).thenReturn("grade");
        when(llmClient.explain(ArgumentMatchers.anyString(), ArgumentMatchers.eq("grade"),
                ArgumentMatchers.<java.util.Map<String, Object>>any(), ArgumentMatchers.any()))
                .thenReturn("SELECT * FROM grade");
        prepareEmptyQuestionMetrics();

        EducationAnalysisQuestionResponseDTO response = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question("请分析成绩").build());

        assertTrue(response.getAnswer().contains("没有已审核成绩记录"));
    }

    @Test
    void shouldReturnSafeFallbackWithoutQueryingForSqlOrBlankQuestion() {
        EducationAnalysisQuestionResponseDTO blank = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question(" ").build());
        EducationAnalysisQuestionResponseDTO sql = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question("SELECT * FROM grade").build());

        assertEquals("keyword", blank.getAnswerSource());
        assertEquals("unsupported", sql.getIntentCode());
        assertTrue(sql.getAnswer().contains("结论："));
        assertTrue(sql.getAnswer().contains("干预建议："));
        verifyNoInteractions(studentMapper, gradeMapper, attendanceMapper, graduationAuditService);
    }

    @Test
    void shouldExplainAcademicRiskWithRequiredSectionsWhenStudentIdentityIsMissing() {
        prepareEmptyQuestionMetrics();

        EducationAnalysisQuestionResponseDTO response = service.answerQuestion(
                EducationAnalysisQuestionRequestDTO.builder().question("学业预警分析").build());

        assertEquals("academic_risk", response.getIntentCode());
        assertEquals("keyword", response.getAnswerSource());
        assertTrue(response.getAnswer().contains("无法确认具体学生身份"));
        assertTrue(response.getAnswer().contains("关键数据："));
        assertTrue(response.getAnswer().contains("命中规则："));
        assertTrue(response.getAnswer().contains("风险原因："));
        assertTrue(response.getAnswer().contains("干预建议："));
        assertTrue(response.getAnswer().length() <= 500);
    }

    private void prepareEmptyQuestionMetrics() {
        when(studentMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Student>>any()))
                .thenReturn(Collections.<Student>emptyList());
        when(gradeMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Grade>>any()))
                .thenReturn(Collections.<Grade>emptyList());
        when(attendanceMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Attendance>>any()))
                .thenReturn(Collections.<Attendance>emptyList());
        when(graduationAuditService.getStatistics(null, null))
                .thenReturn(Collections.<String, Object>emptyMap());
    }

    private Student buildStudent(String studentId, String name) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setName(name);
        student.setStatus("active");
        return student;
    }

    private Grade failedGrade(String studentId, String courseId) {
        Grade grade = new Grade();
        grade.setGradeId(studentId + "-" + courseId);
        grade.setStudentId(studentId);
        grade.setCourseId(courseId);
        grade.setSemesterId("SEM001");
        grade.setStatus("approved");
        grade.setTotalScore(50D);
        grade.setIsPass(false);
        return grade;
    }

    private Grade passedGrade(String studentId, String courseId) {
        Grade grade = new Grade();
        grade.setGradeId(studentId + "-" + courseId + "-pass");
        grade.setStudentId(studentId);
        grade.setCourseId(courseId);
        grade.setSemesterId("SEM001");
        grade.setStatus("approved");
        grade.setTotalScore(80D);
        grade.setIsPass(true);
        return grade;
    }

    private Attendance absence(String studentId) {
        Attendance attendance = new Attendance();
        attendance.setStudentId(studentId);
        attendance.setSemesterId("SEM001");
        attendance.setStatus("absent");
        return attendance;
    }
}
