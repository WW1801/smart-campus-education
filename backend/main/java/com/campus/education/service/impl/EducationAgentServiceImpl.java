package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessException;
import com.campus.education.dto.GraduationAuditVO;
import com.campus.education.dto.agent.AcademicWarningDetailDTO;
import com.campus.education.dto.agent.AcademicWarningListDTO;
import com.campus.education.dto.agent.AcademicWarningProcessUpdateDTO;
import com.campus.education.dto.agent.AcademicWarningRecordDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionRequestDTO;
import com.campus.education.dto.agent.EducationAnalysisQuestionResponseDTO;
import com.campus.education.dto.agent.EducationMetricsOverviewDTO;
import com.campus.education.dto.agent.SemesterGradeTrendDTO;
import com.campus.education.dto.agent.StudentGradeTrendDTO;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.Course;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.Grade;
import com.campus.education.entity.GraduationAudit;
import com.campus.education.entity.Major;
import com.campus.education.entity.Student;
import com.campus.education.entity.StudentCourseSelection;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.ClassMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.CourseScheduleMapper;
import com.campus.education.mapper.GradeMapper;
import com.campus.education.mapper.MajorMapper;
import com.campus.education.mapper.StudentCourseSelectionMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.AcademicWarningRecordService;
import com.campus.education.service.AttendanceService;
import com.campus.education.service.EducationAgentService;
import com.campus.education.service.GradeService;
import com.campus.education.service.GraduationAuditService;
import com.campus.education.service.agent.AcademicWarningAssessment;
import com.campus.education.service.agent.AcademicWarningContext;
import com.campus.education.service.agent.AcademicWarningRuleEngine;
import com.campus.education.service.llm.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 规则版 Agent 实现。所有指标均由固定方法和 MyBatis Plus 条件查询生成。
 */
@Service
public class EducationAgentServiceImpl implements EducationAgentService {

    private static final List<String> ABNORMAL_ATTENDANCE_STATUSES = Arrays.asList("absent", "late", "leave");
    private static final AcademicWarningRuleEngine ACADEMIC_WARNING_RULE_ENGINE = new AcademicWarningRuleEngine();

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private GradeMapper gradeMapper;

    /** 学业预警趋势的唯一成绩数据入口；不允许由 LLM 或自由 SQL 查询。 */
    @Autowired
    private GradeService gradeService;

    @Autowired
    private AttendanceMapper attendanceMapper;

    /** 学业预警详情的固定考勤数据入口；LLM 不参与数据库访问。 */
    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private StudentCourseSelectionMapper selectionMapper;

    @Autowired
    private GraduationAuditService graduationAuditService;

    @Autowired
    private MajorMapper majorMapper;

    @Autowired
    private ClassMapper classMapper;

    @Autowired
    private AcademicWarningRecordService academicWarningRecordService;

    /**
     * LLM is optional at runtime. A missing key or failed request falls back to rule matching.
     */
    @Autowired(required = false)
    private LlmClient llmClient;

    @Override
    public IPage<AcademicWarningListDTO> pageAcademicWarnings(String semesterId, String departmentId,
                                                               String majorId, String classId, String keyword,
                                                               String level, Integer page, Integer pageSize) {
        // 先按固定学生范围和学号/姓名筛选，再复用同一规则引擎计算风险，避免列表与详情口径不一致。
        List<Student> students = filterStudentsByKeyword(
                findStudents(departmentId, majorId, classId), normalize(keyword));
        // 分页参数在 Service 统一兜底并限制最大页大小，Controller 不参与业务计算。
        long current = page == null || page < 1 ? 1L : page;
        long size = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100);
        Page<AcademicWarningListDTO> result = new Page<>(current, size);
        if (students.isEmpty()) {
            result.setTotal(0);
            result.setRecords(Collections.<AcademicWarningListDTO>emptyList());
            return result;
        }

        Set<String> studentIds = new LinkedHashSet<>();
        Set<String> majorIds = new LinkedHashSet<>();
        Set<String> classIds = new LinkedHashSet<>();
        for (Student student : students) {
            studentIds.add(student.getStudentId());
            if (student.getMajorId() != null) {
                majorIds.add(student.getMajorId());
            }
            if (student.getClassId() != null) {
                classIds.add(student.getClassId());
            }
        }

        Map<String, List<Grade>> gradeMap = loadGradesByStudent(studentIds, semesterId);
        Map<String, List<Attendance>> attendanceMap = loadAttendanceByStudent(studentIds, semesterId);
        Map<String, String> auditStatusMap = loadGraduationAuditStatuses(studentIds);
        Map<String, String> majorNameMap = loadMajorNames(majorIds);
        Map<String, String> classNameMap = loadClassNames(classIds);

        List<AcademicWarningListDTO> warnings = new ArrayList<>();
        for (Student student : students) {
            AcademicWarningDetailDTO detail = buildAcademicWarningDetail(
                    student,
                    normalize(semesterId),
                    gradeMap.getOrDefault(student.getStudentId(), Collections.<Grade>emptyList()),
                    attendanceMap.getOrDefault(student.getStudentId(), Collections.<Attendance>emptyList()),
                    auditStatusMap.get(student.getStudentId()),
                    majorNameMap.get(student.getMajorId()),
                    classNameMap.get(student.getClassId()));
            // 学期筛选已在固定成绩、考勤查询中生效；风险等级仅过滤规则计算后的结果。
            if (detail.getTriggeredRules().isEmpty()
                    || (normalize(level) != null && !normalize(level).equals(detail.getRiskLevel()))) {
                continue;
            }
            warnings.add(toAcademicWarningList(student, detail));
        }

        warnings.sort(Comparator
                .comparingInt((AcademicWarningListDTO item) -> riskLevelWeight(item.getRiskLevel())).reversed()
                .thenComparing(AcademicWarningListDTO::getRiskScore, Comparator.reverseOrder())
                .thenComparing(AcademicWarningListDTO::getStudentId));

        result.setTotal(warnings.size());
        long from = (current - 1) * size;
        if (from >= warnings.size()) {
            result.setRecords(Collections.<AcademicWarningListDTO>emptyList());
        } else {
            int to = (int) Math.min(warnings.size(), from + size);
            result.setRecords(warnings.subList((int) from, to));
        }
        return result;
    }

    @Override
    public AcademicWarningDetailDTO getAcademicWarningDetail(String studentId, String semesterId) {
        String normalizedStudentId = normalize(studentId);
        Student student = normalizedStudentId == null ? null : studentMapper.selectById(normalizedStudentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }

        String normalizedSemesterId = normalize(semesterId);
        // 详情只复用固定 Service 数据源，模型和详情接口均不能传入 SQL。
        List<Grade> grades = filterGradesBySemester(
                gradeService.listApprovedGradesByStudent(normalizedStudentId), normalizedSemesterId);
        List<Attendance> attendance = safeList(
                attendanceService.listByStudentForWarning(normalizedStudentId, normalizedSemesterId));
        GraduationAuditVO audit = graduationAuditService.getAuditDetail(normalizedStudentId);
        Major major = student.getMajorId() == null ? null : majorMapper.selectById(student.getMajorId());
        com.campus.education.entity.Class studentClass = student.getClassId() == null
                ? null : classMapper.selectById(student.getClassId());

        return buildAcademicWarningDetail(
                student,
                normalizedSemesterId,
                grades,
                attendance,
                audit == null ? null : audit.getStatus(),
                major == null ? null : major.getName(),
                studentClass == null ? null : studentClass.getName());
    }

    /** 固定成绩服务按学生返回数据；详情在内存中按可选学期收窄，空数据统一回退为空列表。 */
    private List<Grade> filterGradesBySemester(List<Grade> grades, String semesterId) {
        List<Grade> result = new ArrayList<>();
        for (Grade grade : safeList(grades)) {
            if (semesterId == null || semesterId.equals(grade.getSemesterId())) {
                result.add(grade);
            }
        }
        return result;
    }

    /** Mapper 或固定服务无记录时按空列表计算，确保详情仍返回完整零值字段。 */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.<T>emptyList() : values;
    }

    @Override
    public AcademicWarningRecordDTO saveAcademicWarningRecord(String studentId, String semesterId) {
        AcademicWarningDetailDTO detail = getAcademicWarningDetail(studentId, semesterId);
        return academicWarningRecordService.createFromDetail(detail);
    }

    @Override
    public IPage<AcademicWarningRecordDTO> pageAcademicWarningHistory(String studentId, String semesterId,
                                                                      String processStatus, String riskLevel,
                                                                      Integer page, Integer pageSize) {
        return academicWarningRecordService.pageStudentHistory(
                studentId, semesterId, processStatus, riskLevel, page, pageSize);
    }

    @Override
    public AcademicWarningRecordDTO updateAcademicWarningProcess(String recordId, AcademicWarningProcessUpdateDTO request,
                                                                 String processedBy) {
        return academicWarningRecordService.updateProcess(recordId, request, processedBy);
    }

    @Override
    public List<AcademicWarningRecordDTO> saveAcademicWarningSnapshot(String semesterId, String departmentId,
                                                                      String majorId, String classId,
                                                                      String keyword, String level) {
        IPage<AcademicWarningListDTO> page = pageAcademicWarnings(
                semesterId, departmentId, majorId, classId, keyword, level, 1, 100);
        List<AcademicWarningRecordDTO> records = new ArrayList<>();
        for (AcademicWarningListDTO item : page.getRecords()) {
            records.add(saveAcademicWarningRecord(item.getStudentId(), semesterId));
        }
        return records;
    }

    @Override
    public EducationMetricsOverviewDTO getOverview(String semesterId, String departmentId, String majorId, String classId) {
        List<Student> students = findStudents(departmentId, majorId, classId);
        Map<String, Object> gradeMetrics = getGradeMetrics(semesterId, departmentId, majorId, classId);
        Map<String, Object> attendanceMetrics = getAttendanceMetrics(semesterId, departmentId, majorId, classId);
        Map<String, Object> graduationMetrics = graduationAuditService.getStatistics(normalize(majorId), normalize(classId));

        long activeStudentCount = 0;
        for (Student student : students) {
            if ("active".equals(student.getStatus())) {
                activeStudentCount++;
            }
        }

        return EducationMetricsOverviewDTO.builder()
                .studentCount((long) students.size())
                .activeStudentCount(activeStudentCount)
                .gradeRecordCount(numberValue(gradeMetrics.get("total")))
                .failedCourseCount(numberValue(gradeMetrics.get("failedCount")))
                .gradePassRate(doubleValue(gradeMetrics.get("passRate")))
                .attendanceRecordCount(numberValue(attendanceMetrics.get("total")))
                .abnormalAttendanceCount(numberValue(attendanceMetrics.get("abnormalCount")))
                .graduationAuditCount(numberValue(graduationMetrics.get("auditedCount")))
                .approvedGraduationCount(numberValue(graduationMetrics.get("approvedCount")))
                .rejectedGraduationCount(numberValue(graduationMetrics.get("rejectedCount")))
                .graduationApprovalRate(percentValue(graduationMetrics.get("approvalRate")))
                .build();
    }

    @Override
    public Map<String, Object> getGradeMetrics(String semesterId, String departmentId, String majorId, String classId) {
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Grade::getStatus, "approved");
        applyGradeScope(wrapper, findStudentIds(departmentId, majorId, classId), hasStudentScope(departmentId, majorId, classId));
        if (normalize(semesterId) != null) {
            wrapper.eq(Grade::getSemesterId, normalize(semesterId));
        }

        List<Grade> grades = gradeMapper.selectList(wrapper);
        double totalScore = 0;
        int scoredCount = 0;
        long passCount = 0;
        long failedCount = 0;
        long excellentCount = 0;
        long goodCount = 0;
        long mediumCount = 0;
        long passMarkCount = 0;
        double maxScore = 0;
        double minScore = 0;
        Map<String, Double> courseScoreSum = new LinkedHashMap<>();
        Map<String, Integer> courseScoreCount = new LinkedHashMap<>();

        for (Grade grade : grades) {
            if (Boolean.TRUE.equals(grade.getIsPass())) {
                passCount++;
            } else {
                failedCount++;
            }
            if (grade.getTotalScore() == null) {
                continue;
            }
            double score = grade.getTotalScore();
            totalScore += score;
            scoredCount++;
            maxScore = scoredCount == 1 ? score : Math.max(maxScore, score);
            minScore = scoredCount == 1 ? score : Math.min(minScore, score);
            if (score >= 90) {
                excellentCount++;
            } else if (score >= 80) {
                goodCount++;
            } else if (score >= 70) {
                mediumCount++;
            } else if (score >= 60) {
                passMarkCount++;
            }
            courseScoreSum.put(grade.getCourseId(), courseScoreSum.getOrDefault(grade.getCourseId(), 0D) + score);
            courseScoreCount.put(grade.getCourseId(), courseScoreCount.getOrDefault(grade.getCourseId(), 0) + 1);
        }

        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("excellent", excellentCount);
        distribution.put("good", goodCount);
        distribution.put("medium", mediumCount);
        distribution.put("pass", passMarkCount);
        distribution.put("fail", failedCount);

        Map<String, Course> courseMap = findCourses(courseScoreSum.keySet());
        List<Map<String, Object>> courseAverageList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : courseScoreSum.entrySet()) {
            Course course = courseMap.get(entry.getKey());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", entry.getKey());
            row.put("courseName", course == null ? entry.getKey() : course.getName());
            double averageScore = round(entry.getValue() / courseScoreCount.get(entry.getKey()));
            row.put("averageScore", averageScore);
            row.put("name", course == null ? entry.getKey() : course.getName());
            row.put("avg", averageScore);
            row.put("studentCount", courseScoreCount.get(entry.getKey()));
            courseAverageList.add(row);
        }

        double passRate = grades.isEmpty() ? 0 : round(passCount * 100D / grades.size());
        double excellentRate = grades.isEmpty() ? 0 : round(excellentCount * 100D / grades.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "grade");
        result.put("total", grades.size());
        result.put("averageScore", scoredCount == 0 ? 0 : round(totalScore / scoredCount));
        result.put("average", scoredCount == 0 ? 0 : round(totalScore / scoredCount));
        result.put("maxScore", scoredCount == 0 ? 0 : maxScore);
        result.put("minScore", scoredCount == 0 ? 0 : minScore);
        result.put("passCount", passCount);
        result.put("failedCount", failedCount);
        result.put("passRate", passRate);
        result.put("excellentRate", excellentRate);
        result.put("distribution", distribution);
        result.put("courseAverageList", courseAverageList);
        result.put("courseAvgList", courseAverageList);
        result.put("explanation", grades.isEmpty()
                ? "当前筛选范围没有已审核成绩记录。"
                : "已审核成绩共 " + grades.size() + " 条，成绩通过率为 " + passRate + "% 。");
        return result;
    }

    @Override
    public Map<String, Object> getAttendanceMetrics(String semesterId, String departmentId, String majorId, String classId) {
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        applyAttendanceScope(wrapper, findStudentIds(departmentId, majorId, classId), hasStudentScope(departmentId, majorId, classId));
        if (normalize(semesterId) != null) {
            wrapper.eq(Attendance::getSemesterId, normalize(semesterId));
        }
        List<Attendance> records = attendanceMapper.selectList(wrapper);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        Set<String> abnormalStudentIds = new HashSet<>();
        long abnormalCount = 0;
        for (Attendance record : records) {
            String status = record.getStatus() == null ? "unknown" : record.getStatus();
            statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
            if (ABNORMAL_ATTENDANCE_STATUSES.contains(status)) {
                abnormalCount++;
                abnormalStudentIds.add(record.getStudentId());
            }
        }
        double abnormalRate = records.isEmpty() ? 0 : round(abnormalCount * 100D / records.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "attendance");
        result.put("total", records.size());
        result.put("normalCount", records.size() - abnormalCount);
        result.put("abnormalCount", abnormalCount);
        result.put("abnormalStudentCount", abnormalStudentIds.size());
        result.put("abnormalRate", abnormalRate);
        result.put("statusCounts", statusCounts);
        result.put("explanation", records.isEmpty()
                ? "当前筛选范围没有考勤记录。"
                : "考勤记录共 " + records.size() + " 条，其中异常记录 " + abnormalCount + " 条，占比 " + abnormalRate + "% 。");
        return result;
    }

    @Override
    public Map<String, Object> getCourseLoadMetrics(String semesterId, String departmentId, String majorId, String classId) {
        LambdaQueryWrapper<CourseSchedule> scheduleWrapper = new LambdaQueryWrapper<>();
        if (normalize(semesterId) != null) {
            scheduleWrapper.eq(CourseSchedule::getSemesterId, normalize(semesterId));
        }
        if (normalize(classId) != null) {
            scheduleWrapper.eq(CourseSchedule::getClassId, normalize(classId));
        }
        List<CourseSchedule> schedules = courseScheduleMapper.selectList(scheduleWrapper);
        Set<String> scheduleIds = new LinkedHashSet<>();
        Set<String> courseIds = new LinkedHashSet<>();
        for (CourseSchedule schedule : schedules) {
            scheduleIds.add(schedule.getScheduleId());
            courseIds.add(schedule.getCourseId());
        }

        Map<String, Integer> selectedCounts = countSelections(scheduleIds, semesterId, departmentId, majorId, classId);
        Map<String, Course> courseMap = findCourses(courseIds);
        int totalCapacity = 0;
        int totalSelectedCount = 0;
        List<Map<String, Object>> courseLoads = new ArrayList<>();
        for (CourseSchedule schedule : schedules) {
            int capacity = schedule.getMaxStudents() == null ? 0 : schedule.getMaxStudents();
            int selectedCount = selectedCounts.getOrDefault(schedule.getScheduleId(), 0);
            totalCapacity += capacity;
            totalSelectedCount += selectedCount;
            Course course = courseMap.get(schedule.getCourseId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("scheduleId", schedule.getScheduleId());
            row.put("courseId", schedule.getCourseId());
            row.put("courseName", course == null ? schedule.getCourseId() : course.getName());
            row.put("teacherId", schedule.getTeacherId());
            row.put("classId", schedule.getClassId());
            row.put("capacity", capacity);
            row.put("selectedCount", selectedCount);
            row.put("loadRate", capacity == 0 ? 0 : round(selectedCount * 100D / capacity));
            courseLoads.add(row);
        }
        double loadRate = totalCapacity == 0 ? 0 : round(totalSelectedCount * 100D / totalCapacity);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "course_load");
        result.put("scheduleCount", schedules.size());
        result.put("courseCount", courseIds.size());
        result.put("selectedCount", totalSelectedCount);
        result.put("totalCapacity", totalCapacity);
        result.put("loadRate", loadRate);
        result.put("courseLoads", courseLoads);
        result.put("explanation", schedules.isEmpty()
                ? "当前筛选范围没有排课记录。"
                : "共统计 " + schedules.size() + " 个排课班，选课负载率为 " + loadRate + "% 。");
        return result;
    }

    @Override
    public List<Map<String, Object>> getMajorStudentSummary(String semesterId, String departmentId,
                                                             String majorId, String classId) {
        // 先按页面筛选范围取学生，再按已定义的 majorId 分组，避免开放任意字段聚合。
        Map<String, List<Student>> studentsByMajor = new LinkedHashMap<>();
        for (Student student : findStudents(departmentId, majorId, classId)) {
            String key = normalize(student.getMajorId());
            studentsByMajor.computeIfAbsent(key == null ? "unassigned" : key, ignored -> new ArrayList<Student>())
                    .add(student);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Student>> entry : studentsByMajor.entrySet()) {
            String currentMajorId = "unassigned".equals(entry.getKey()) ? null : entry.getKey();
            Major major = currentMajorId == null ? null : majorMapper.selectById(currentMajorId);
            long activeCount = 0;
            for (Student student : entry.getValue()) {
                if ("active".equals(student.getStatus())) {
                    activeCount++;
                }
            }

            // 复用学业预警规则引擎的总数，保证概览页与预警 Agent 的口径一致。
            long warningCount = currentMajorId == null ? 0 : pageAcademicWarnings(
                    semesterId, departmentId, currentMajorId, classId, null, null, 1, 1).getTotal();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("majorId", currentMajorId);
            row.put("majorName", major == null ? "未分配专业" : major.getName());
            row.put("studentCount", entry.getValue().size());
            row.put("activeStudentCount", activeCount);
            row.put("warningStudentCount", warningCount);
            row.put("warningRate", entry.getValue().isEmpty() ? 0 : round(warningCount * 100D / entry.getValue().size()));
            result.add(row);
        }
        result.sort(Comparator.<Map<String, Object>>comparingLong(
                item -> numberValue(item.get("studentCount"))).reversed());
        return result;
    }

    @Override
    public Map<String, Object> getAcademicRiskAnalysis(String semesterId, String departmentId, String majorId, String classId) {
        IPage<AcademicWarningListDTO> warnings = pageAcademicWarnings(
                semesterId, departmentId, majorId, classId, null, null, 1, 100);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AcademicWarningListDTO warning : warnings.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", warning.getStudentId());
            row.put("studentName", warning.getStudentName());
            row.put("riskLevel", warning.getRiskLevel());
            row.put("riskScore", warning.getRiskScore());
            row.put("riskReason", warning.getRiskReason());
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "academic_risk");
        result.put("riskStudentCount", warnings.getTotal());
        result.put("riskStudents", rows);
        result.put("explanation", warnings.getTotal() == 0 ? "No students triggered academic-risk rules."
                : warnings.getTotal() + " students triggered fixed academic-risk rules.");
        return result;
    }

    @Override
    public Map<String, Object> getStudentGradeTrend(String studentId) {
        String id = normalize(studentId);
        List<Grade> grades = id == null ? Collections.<Grade>emptyList() : gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>().eq(Grade::getStudentId, id).eq(Grade::getStatus, "approved"));
        Map<String, Double> sums = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Grade grade : grades) {
            if (grade.getTotalScore() != null) {
                String semester = grade.getSemesterId() == null ? "unknown" : grade.getSemesterId();
                sums.put(semester, sums.getOrDefault(semester, 0D) + grade.getTotalScore());
                counts.put(semester, counts.getOrDefault(semester, 0) + 1);
            }
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String semester : sums.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("semesterId", semester);
            row.put("averageScore", round(sums.get(semester) / counts.get(semester)));
            row.put("gradeCount", counts.get(semester));
            trend.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "grade_trend");
        result.put("studentId", id);
        result.put("trend", trend);
        result.put("explanation", id == null ? "studentId is required for grade trend analysis."
                : trend.isEmpty() ? "No approved grades found for this student." : "Grade trend is grouped by semester.");
        return result;
    }

    @Override
    public StudentGradeTrendDTO getAcademicWarningGradeTrend(String studentId) {
        String id = normalize(studentId);
        Student student = id == null ? null : studentMapper.selectById(id);
        if (student == null || normalize(student.getName()) == null) {
            return StudentGradeTrendDTO.builder()
                    .studentId(id)
                    .semesters(Collections.<SemesterGradeTrendDTO>emptyList())
                    .trendDirection("数据不足")
                    .trendConclusion("未找到可核验的学生姓名，无法确认学生身份并生成个人成绩趋势结论。")
                    .riskChange("无法判断")
                    .dataNotice("请提供有效学号，并确认学生姓名后重试。")
                    .build();
        }

        // 固定 Service 仅查询该学生的已审核成绩；DeepSeek 不参与查询或趋势计算。
        List<Grade> grades = gradeService.listApprovedGradesByStudent(id);
        Map<String, List<Grade>> gradesBySemester = new LinkedHashMap<>();
        for (Grade grade : grades) {
            String semesterId = normalize(grade.getSemesterId());
            if (semesterId != null) {
                gradesBySemester.computeIfAbsent(semesterId, key -> new ArrayList<>()).add(grade);
            }
        }

        List<String> semesterIds = new ArrayList<>(gradesBySemester.keySet());
        Collections.sort(semesterIds);
        List<SemesterGradeTrendDTO> semesters = new ArrayList<>();
        for (String semesterId : semesterIds) {
            List<Grade> semesterGrades = gradesBySemester.get(semesterId);
            double scoreSum = 0D;
            int scoredCourseCount = 0;
            int failedCourseCount = 0;
            Set<String> courseIds = new LinkedHashSet<>();
            for (Grade grade : semesterGrades) {
                if (grade.getCourseId() != null) {
                    courseIds.add(grade.getCourseId());
                }
                if (grade.getTotalScore() != null) {
                    scoreSum += grade.getTotalScore();
                    scoredCourseCount++;
                }
                if (isFailedGrade(grade)) {
                    failedCourseCount++;
                }
            }
            semesters.add(SemesterGradeTrendDTO.builder()
                    .semesterId(semesterId)
                    .averageScore(scoredCourseCount == 0 ? 0D : round(scoreSum / scoredCourseCount))
                    .failedCourseCount(failedCourseCount)
                    .courseCount(courseIds.size())
                    .build());
        }

        return buildAcademicWarningGradeTrend(student, semesters);
    }

    private StudentGradeTrendDTO buildAcademicWarningGradeTrend(Student student,
                                                                  List<SemesterGradeTrendDTO> semesters) {
        if (semesters.size() < 2) {
            return StudentGradeTrendDTO.builder()
                    .studentId(student.getStudentId())
                    .studentName(student.getName())
                    .semesters(semesters)
                    .trendDirection("数据不足")
                    .trendConclusion("已审核成绩学期不足 2 个，暂无法比较成绩趋势。")
                    .riskChange("无法判断")
                    .dataNotice("需至少两个有已审核成绩的学期。")
                    .build();
        }

        SemesterGradeTrendDTO previous = semesters.get(semesters.size() - 2);
        SemesterGradeTrendDTO current = semesters.get(semesters.size() - 1);
        double scoreDelta = round(current.getAverageScore() - previous.getAverageScore());
        // 平均分变化达到 3 分才判为上升或下降，避免正常波动触发预警变化。
        String direction = scoreDelta >= 3D ? "上升" : scoreDelta <= -3D ? "下降" : "基本稳定";
        int failedDelta = current.getFailedCourseCount() - previous.getFailedCourseCount();
        String riskChange;
        if (failedDelta > 0 || (failedDelta == 0 && "下降".equals(direction))) {
            riskChange = "风险上升";
        } else if (failedDelta < 0 || (failedDelta == 0 && "上升".equals(direction))) {
            riskChange = "风险下降";
        } else {
            riskChange = "风险基本稳定";
        }
        String trendConclusion = "最近两学期平均分从 " + previous.getAverageScore() + " 分变为 "
                + current.getAverageScore() + " 分（" + (scoreDelta >= 0 ? "+" : "") + scoreDelta
                + "），成绩趋势" + direction + "。";
        return StudentGradeTrendDTO.builder()
                .studentId(student.getStudentId())
                .studentName(student.getName())
                .semesters(semesters)
                .trendDirection(direction)
                .trendConclusion(trendConclusion)
                .riskChange(riskChange)
                .build();
    }

    @Override
    public Map<String, Object> getFailedCourseAnalysis(String semesterId, String departmentId, String majorId,
                                                        String classId, String studentId) {
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<Grade>().eq(Grade::getStatus, "approved");
        applyGradeScope(wrapper, findStudentIds(departmentId, majorId, classId), hasStudentScope(departmentId, majorId, classId));
        wrapper.eq(normalize(semesterId) != null, Grade::getSemesterId, normalize(semesterId));
        wrapper.eq(normalize(studentId) != null, Grade::getStudentId, normalize(studentId));
        Map<String, Integer> failedCounts = new LinkedHashMap<>();
        for (Grade grade : gradeMapper.selectList(wrapper)) {
            if (isFailedGrade(grade)) {
                failedCounts.put(grade.getCourseId(), failedCounts.getOrDefault(grade.getCourseId(), 0) + 1);
            }
        }
        Map<String, Course> courses = findCourses(failedCounts.keySet());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : failedCounts.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", entry.getKey());
            row.put("courseName", courses.containsKey(entry.getKey()) ? courses.get(entry.getKey()).getName() : entry.getKey());
            row.put("failedCount", entry.getValue());
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "failed_course");
        result.put("failedCourseCount", rows.size());
        result.put("courses", rows);
        result.put("explanation", rows.isEmpty() ? "No failed approved grades found." : "Failed courses are aggregated by course.");
        return result;
    }

    @Override
    public Map<String, Object> getAttendanceAbnormalAnalysis(String semesterId, String departmentId, String majorId,
                                                              String classId, String studentId) {
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        applyAttendanceScope(wrapper, findStudentIds(departmentId, majorId, classId), hasStudentScope(departmentId, majorId, classId));
        wrapper.eq(normalize(semesterId) != null, Attendance::getSemesterId, normalize(semesterId));
        wrapper.eq(normalize(studentId) != null, Attendance::getStudentId, normalize(studentId));
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Attendance attendance : attendanceMapper.selectList(wrapper)) {
            if (ABNORMAL_ATTENDANCE_STATUSES.contains(attendance.getStatus())) {
                counts.put(attendance.getStudentId(), counts.getOrDefault(attendance.getStudentId(), 0) + 1);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", entry.getKey());
            row.put("abnormalCount", entry.getValue());
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "attendance_abnormal");
        result.put("abnormalStudentCount", rows.size());
        result.put("students", rows);
        result.put("explanation", rows.isEmpty() ? "No abnormal attendance records found." : "Abnormal attendance is aggregated by student.");
        return result;
    }

    @Override
    public Map<String, Object> getStudentComparison(String studentId, String compareStudentId, String semesterId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : Arrays.asList(normalize(studentId), normalize(compareStudentId))) {
            if (id == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", id);
            row.put("gradeTrend", getStudentGradeTrend(id).get("trend"));
            row.put("attendance", getAttendanceAbnormalAnalysis(semesterId, null, null, null, id).get("students"));
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricCode", "student_comparison");
        result.put("students", rows);
        result.put("explanation", rows.size() == 2 ? "The two students use identical fixed grade and attendance metrics."
                : "studentId and compareStudentId are both required for comparison.");
        return result;
    }

    @Override
    public Map<String, Object> getDashboard(String semesterId, String departmentId, String majorId, String classId) {
        EducationMetricsOverviewDTO overview = getOverview(semesterId, departmentId, majorId, classId);
        Map<String, Object> gradeMetrics = getGradeMetrics(semesterId, departmentId, majorId, classId);
        Map<String, Object> attendanceMetrics = getAttendanceMetrics(semesterId, departmentId, majorId, classId);
        Map<String, Object> courseLoadMetrics = getCourseLoadMetrics(semesterId, departmentId, majorId, classId);
        Map<String, Object> graduationMetrics = graduationAuditService.getStatistics(normalize(majorId), normalize(classId));

        Map<String, Object> overviewMap = toOverviewMap(overview);
        overviewMap.put("riskSignal", buildRiskSignal(overview));
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("semesterId", normalize(semesterId));
        filters.put("departmentId", normalize(departmentId));
        filters.put("majorId", normalize(majorId));
        filters.put("classId", normalize(classId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agentName", "智能教务数据分析 Agent");
        result.put("status", "ready");
        result.put("summary", "已使用固定指标查询生成教务概览、成绩、考勤和课程负载分析结果。");
        result.put("filters", filters);
        result.put("dataOverview", overviewMap);
        result.put("gradeAnalysis", gradeMetrics);
        result.put("attendanceAnalysis", attendanceMetrics);
        result.put("courseLoadAnalysis", courseLoadMetrics);
        result.put("graduationAnalysis", graduationMetrics);
        result.put("insights", buildInsights(overview, courseLoadMetrics));
        result.put("modules", buildModules());
        result.put("nextApis", Arrays.asList(
                "GET /agent/metrics/overview",
                "GET /agent/metrics/grade",
                "GET /agent/metrics/attendance",
                "GET /agent/metrics/course",
                "POST /agent/chat"
        ));
        return result;
    }

    @Override
    public EducationAnalysisQuestionResponseDTO answerQuestion(EducationAnalysisQuestionRequestDTO request) {
        String question = request == null ? null : normalize(request.getQuestion());
        if (!isSafeAndRelevantQuestion(question)) {
            return buildSafeQuestionFallback();
        }

        // 成绩趋势直接由关键词路由到固定 Service，DeepSeek 仅可解释已计算的指标。
        IntentResolution intentResolution = isGradeTrendQuestion(question)
                ? new IntentResolution("grade_trend", "keyword")
                : isMajorSummaryQuestion(question)
                ? new IntentResolution("major_summary", "keyword")
                : resolveIntentWithFallback(question);
        String intent = intentResolution.intent;
        String semesterId = request.getSemesterId();
        String departmentId = request.getDepartmentId();
        String majorId = request.getMajorId();
        String classId = request.getClassId();
        String studentId = request.getStudentId();
        String compareStudentId = request.getCompareStudentId();
        EducationMetricsOverviewDTO overview = getOverview(semesterId, departmentId, majorId, classId);
        Map<String, Object> metrics;
        List<Map<String, Object>> detailRows;
        String answer;
        String intentName;

        if ("major_summary".equals(intent)) {
            detailRows = getMajorStudentSummary(semesterId, departmentId, majorId, classId);
            metrics = new LinkedHashMap<>();
            metrics.put("metricCode", "major_summary");
            metrics.put("majors", detailRows);
            intentName = "专业学生统计";
            answer = buildMajorSummaryExplanation(detailRows);
        } else if ("grade".equals(intent)) {
            metrics = getGradeMetrics(semesterId, departmentId, majorId, classId);
            detailRows = listValue(metrics.get("courseAverageList"));
            intentName = "成绩分析";
            answer = metrics.get("explanation").toString();
        } else if ("attendance".equals(intent)) {
            metrics = getAttendanceMetrics(semesterId, departmentId, majorId, classId);
            detailRows = Collections.emptyList();
            intentName = "考勤分析";
            answer = metrics.get("explanation").toString();
        } else if ("course_load".equals(intent)) {
            metrics = getCourseLoadMetrics(semesterId, departmentId, majorId, classId);
            detailRows = listValue(metrics.get("courseLoads"));
            intentName = "课程负载分析";
            answer = metrics.get("explanation").toString();
        } else if ("academic_risk".equals(intent)) {
            metrics = getAcademicRiskAnalysis(semesterId, departmentId, majorId, classId);
            detailRows = listValue(metrics.get("riskStudents"));
            intentName = "学业风险分析";
            // 预警解释只使用固定查询结果组装，不交给模型补写个人事实。
            answer = buildAcademicWarningExplanation(studentId, semesterId, metrics);
        } else if ("grade_trend".equals(intent)) {
            StudentGradeTrendDTO gradeTrend = getAcademicWarningGradeTrend(studentId);
            metrics = toAcademicWarningGradeTrendMetrics(gradeTrend);
            detailRows = toAcademicWarningGradeTrendRows(gradeTrend.getSemesters());
            intentName = "学生成绩趋势";
            // 趋势和风险变化由固定 Service 计算；模型只能基于 metrics 解释该结果。
            answer = gradeTrend.getTrendConclusion() + " 风险变化：" + gradeTrend.getRiskChange() + "。";
        } else if ("failed_course".equals(intent)) {
            metrics = getFailedCourseAnalysis(semesterId, departmentId, majorId, classId, studentId);
            detailRows = listValue(metrics.get("courses"));
            intentName = "不及格课程分析";
            answer = metrics.get("explanation").toString();
        } else if ("attendance_abnormal".equals(intent)) {
            metrics = getAttendanceAbnormalAnalysis(semesterId, departmentId, majorId, classId, studentId);
            detailRows = listValue(metrics.get("students"));
            intentName = "考勤异常分析";
            answer = metrics.get("explanation").toString();
        } else if ("student_comparison".equals(intent)) {
            metrics = getStudentComparison(studentId, compareStudentId, semesterId);
            detailRows = listValue(metrics.get("students"));
            intentName = "学生对比分析";
            answer = metrics.get("explanation").toString();
        } else {
            metrics = toOverviewMap(overview);
            detailRows = Collections.emptyList();
            intentName = "教务概览";
            answer = "当前范围共有 " + overview.getStudentCount() + " 名学生，已审核成绩 "
                    + overview.getGradeRecordCount() + " 条，异常考勤 " + overview.getAbnormalAttendanceCount() + " 条。";
        }

        // 学业预警必须包含固定字段，因此保留可核验的关键词回退结果。
        AnswerResolution answerResolution = "academic_risk".equals(intent)
                ? new AnswerResolution(answer, "keyword")
                : explainWithFallback(question, intent, metrics, overview, answer);
        answer = answerResolution.answer;

        return EducationAnalysisQuestionResponseDTO.builder()
                .intentCode(intent)
                .intentName(intentName)
                .answer(answer)
                .intentSource(intentResolution.source)
                .answerSource(answerResolution.source)
                .metricsOverview(overview)
                .insights(Collections.singletonList("本结果由 " + intentName + " 固定查询模板生成。"))
                .detailRows(detailRows)
                .build();
    }

    private Map<String, Object> toAcademicWarningGradeTrendMetrics(StudentGradeTrendDTO trend) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("metricCode", "grade_trend");
        metrics.put("studentId", trend.getStudentId());
        metrics.put("studentName", trend.getStudentName());
        metrics.put("semesters", trend.getSemesters());
        metrics.put("trendDirection", trend.getTrendDirection());
        metrics.put("trendConclusion", trend.getTrendConclusion());
        metrics.put("riskChange", trend.getRiskChange());
        metrics.put("dataNotice", trend.getDataNotice());
        return metrics;
    }

    private List<Map<String, Object>> toAcademicWarningGradeTrendRows(List<SemesterGradeTrendDTO> semesters) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SemesterGradeTrendDTO semester : semesters == null
                ? Collections.<SemesterGradeTrendDTO>emptyList() : semesters) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("semesterId", semester.getSemesterId());
            row.put("averageScore", semester.getAverageScore());
            row.put("failedCourseCount", semester.getFailedCourseCount());
            row.put("courseCount", semester.getCourseCount());
            rows.add(row);
        }
        return rows;
    }

    private EducationAnalysisQuestionResponseDTO buildSafeQuestionFallback() {
        return EducationAnalysisQuestionResponseDTO.builder()
                .intentCode("unsupported")
                .intentName("安全回退")
                .answer("结论：仅支持教务指标分析，无法处理空白、SQL 或无关问题。\n关键数据：未执行数据查询。\n命中规则：无。\n风险原因：输入不在允许范围。\n干预建议：请提出成绩、考勤、课程或学业预警相关问题。")
                .intentSource("keyword")
                .answerSource("keyword")
                .insights(Collections.singletonList("为防止 SQL 注入和无关查询，未调用数据库。"))
                .detailRows(Collections.<Map<String, Object>>emptyList())
                .build();
    }

    private boolean isSafeAndRelevantQuestion(String question) {
        if (question == null || question.length() > 500) {
            return false;
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        if (normalized.matches(".*(select|insert|update|delete|drop|alter|truncate|create|--|/\\*|\\*/|;).*")) {
            return false;
        }
        return containsAny(question, "学生", "成绩", "考勤", "课程", "学业", "预警", "风险", "毕业", "挂科", "不及格", "缺勤", "迟到", "选课", "班级", "专业", "分数");
    }

    private String buildAcademicWarningExplanation(String studentId, String semesterId, Map<String, Object> metrics) {
        String normalizedStudentId = normalize(studentId);
        if (normalizedStudentId == null) {
            return "结论：当前范围命中 " + metrics.get("riskStudentCount")
                    + " 名预警学生；未提供可核验的学生姓名，无法确认具体学生身份。\n"
                    + "关键数据：预警学生 " + metrics.get("riskStudentCount") + " 名。\n"
                    + "命中规则：未指定已核验学生，不展示个人规则。\n"
                    + "风险原因：只能查看范围汇总，不能将风险归属至某一学生。\n"
                    + "干预建议：请提供学号并确认学生姓名后再进行个人预警分析。";
        }
        Student student = studentMapper.selectById(normalizedStudentId);
        if (student == null || normalize(student.getName()) == null) {
            return "结论：未找到可核验的学生姓名，无法确认学生身份。\n"
                    + "关键数据：未展示个人数据。\n命中规则：无。\n"
                    + "风险原因：学号对应学生不存在或姓名缺失。\n干预建议：请核对学号与学生姓名。";
        }
        AcademicWarningDetailDTO detail = getAcademicWarningDetail(normalizedStudentId, semesterId);
        // 字段分别限长，既保留五段必填信息，也保证整体答复不超过 500 字。
        String rules = detail.getTriggeredRules().isEmpty() ? "无" : limitText(String.join("；", detail.getTriggeredRules()), 120);
        String riskReason = limitText(detail.getRiskReason(), 120);
        String suggestion = limitText(detail.getInterventionSuggestion(), 120);
        return "结论：" + detail.getStudentName() + "（" + detail.getStudentId() + "）为"
                + detail.getRiskLevel() + "风险，风险分 " + detail.getRiskScore() + "。\n"
                + "关键数据：挂科 " + detail.getFailedCourseCount() + " 门；缺勤 "
                + detail.getAbsentCount() + " 次；迟到 " + detail.getLateCount() + " 次；毕业审核 " + detail.getGraduationAuditStatus() + "。\n"
                + "命中规则：" + rules + "。\n"
                + "风险原因：" + riskReason + "。\n"
                + "干预建议：" + suggestion + "。";
    }

    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "无" : text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private List<Student> filterStudentsByKeyword(List<Student> students, String keyword) {
        if (keyword == null) {
            return students;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        List<Student> result = new ArrayList<>();
        for (Student student : students) {
            String studentId = student.getStudentId() == null ? "" : student.getStudentId().toLowerCase(Locale.ROOT);
            String name = student.getName() == null ? "" : student.getName().toLowerCase(Locale.ROOT);
            if (studentId.contains(normalizedKeyword) || name.contains(normalizedKeyword)) {
                result.add(student);
            }
        }
        return result;
    }

    private Map<String, List<Grade>> loadGradesByStudent(Set<String> studentIds, String semesterId) {
        // 批量读取指定学生的已审核成绩；学期为空时按跨学期汇总计算。
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<Grade>()
                .in(Grade::getStudentId, studentIds)
                .eq(Grade::getStatus, "approved")
                .eq(normalize(semesterId) != null, Grade::getSemesterId, normalize(semesterId));
        Map<String, List<Grade>> result = new HashMap<>();
        for (Grade grade : gradeMapper.selectList(wrapper)) {
            result.computeIfAbsent(grade.getStudentId(), key -> new ArrayList<>()).add(grade);
        }
        return result;
    }

    private Map<String, List<Attendance>> loadAttendanceByStudent(Set<String> studentIds, String semesterId) {
        // 批量读取指定学生考勤；缺勤规则只统计 status=absent 的记录。
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<Attendance>()
                .in(Attendance::getStudentId, studentIds)
                .eq(normalize(semesterId) != null, Attendance::getSemesterId, normalize(semesterId));
        Map<String, List<Attendance>> result = new HashMap<>();
        for (Attendance record : attendanceMapper.selectList(wrapper)) {
            result.computeIfAbsent(record.getStudentId(), key -> new ArrayList<>()).add(record);
        }
        return result;
    }

    private Map<String, String> loadGraduationAuditStatuses(Set<String> studentIds) {
        Map<String, String> result = new HashMap<>();
        // 毕业审核按学生维度读取，rejected 会直接触发高风险规则。
        List<GraduationAudit> audits = graduationAuditService.list(
                new LambdaQueryWrapper<GraduationAudit>().in(GraduationAudit::getStudentId, studentIds));
        for (GraduationAudit audit : audits) {
            result.put(audit.getStudentId(), audit.getStatus());
        }
        return result;
    }

    private Map<String, String> loadMajorNames(Set<String> majorIds) {
        if (majorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (Major major : majorMapper.selectBatchIds(majorIds)) {
            result.put(major.getMajorId(), major.getName());
        }
        return result;
    }

    private Map<String, String> loadClassNames(Set<String> classIds) {
        if (classIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (com.campus.education.entity.Class studentClass : classMapper.selectBatchIds(classIds)) {
            result.put(studentClass.getClassId(), studentClass.getName());
        }
        return result;
    }

    private AcademicWarningDetailDTO buildAcademicWarningDetail(Student student, String semesterId,
                                                                  List<Grade> grades,
                                                                  List<Attendance> attendance,
                                                                  String graduationAuditStatus,
                                                                  String majorName,
                                                                  String className) {
        // 风险计算只使用成绩、缺勤和毕业审核三类固定数据，不接受自由 SQL。
        int failedCourseCount = countFailedCourses(grades);
        int absentCount = countAttendanceStatus(attendance, "absent");
        int lateCount = countAttendanceStatus(attendance, "late");
        // 未产生毕业审核记录时回退为 unaudited，不触发毕业审核未通过规则。
        String normalizedAuditStatus = graduationAuditStatus == null ? "unaudited" : graduationAuditStatus;
        AcademicWarningContext context = AcademicWarningContext.builder()
                .failedCourseCount(failedCourseCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .graduationAuditStatus(normalizedAuditStatus)
                .build();
        // 规则引擎负责风险等级优先级：多规则命中时取最高等级，风险分累加并封顶。
        AcademicWarningAssessment assessment = ACADEMIC_WARNING_RULE_ENGINE.assess(context);

        return AcademicWarningDetailDTO.builder()
                .studentId(student.getStudentId())
                .studentName(student.getName())
                .majorName(majorName)
                .className(className)
                .semesterId(semesterId)
                .riskLevel(assessment.getRiskLevel())
                .riskScore(assessment.getRiskScore())
                .failedCourseCount(failedCourseCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .graduationAuditStatus(normalizedAuditStatus)
                .triggeredRules(assessment.getTriggeredRules())
                .riskReason(assessment.getRiskReason())
                .interventionSuggestion(assessment.getInterventionSuggestion())
                .build();
    }

    private AcademicWarningListDTO toAcademicWarningList(Student student, AcademicWarningDetailDTO detail) {
        return AcademicWarningListDTO.builder()
                .studentId(student.getStudentId())
                .studentName(student.getName())
                .departmentId(student.getDepartmentId())
                .majorId(student.getMajorId())
                .majorName(detail.getMajorName())
                .classId(student.getClassId())
                .className(detail.getClassName())
                .riskLevel(detail.getRiskLevel())
                .riskScore(detail.getRiskScore())
                .riskReason(detail.getRiskReason())
                .triggeredRules(detail.getTriggeredRules())
                .triggeredRuleCount(detail.getTriggeredRules().size())
                .build();
    }

    private int countFailedCourses(List<Grade> grades) {
        Map<String, Grade> latestGradeByCourse = new HashMap<>();
        for (Grade grade : grades) {
            // 同一课程多条成绩只取最新记录，避免补考通过后仍被旧不及格记录命中。
            String courseKey = grade.getCourseId() == null ? grade.getGradeId() : grade.getCourseId();
            Grade previous = latestGradeByCourse.get(courseKey);
            if (previous == null || isLaterGrade(grade, previous)) {
                latestGradeByCourse.put(courseKey, grade);
            }
        }
        int failedCount = 0;
        for (Grade grade : latestGradeByCourse.values()) {
            if (isFailedGrade(grade)) {
                failedCount++;
            }
        }
        return failedCount;
    }

    private boolean isLaterGrade(Grade candidate, Grade previous) {
        if (candidate.getUpdatedAt() != null || previous.getUpdatedAt() != null) {
            return previous.getUpdatedAt() == null
                    || (candidate.getUpdatedAt() != null && candidate.getUpdatedAt().isAfter(previous.getUpdatedAt()));
        }
        return previous.getCreatedAt() == null
                || (candidate.getCreatedAt() != null && candidate.getCreatedAt().isAfter(previous.getCreatedAt()));
    }

    private boolean isFailedGrade(Grade grade) {
        if (grade.getIsPass() != null) {
            return !grade.getIsPass();
        }
        return grade.getTotalScore() != null && grade.getTotalScore() < 60;
    }

    private int countAttendanceStatus(List<Attendance> attendance, String status) {
        int count = 0;
        for (Attendance record : attendance) {
            if (status.equals(record.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int riskLevelWeight(String level) {
        return ACADEMIC_WARNING_RULE_ENGINE.riskLevelWeight(level);
    }

    private String resolveIntent(String question) {
        if (containsAny(question, "学生对比", "对比学生", "比较学生")) {
            return "student_comparison";
        }
        if (containsAny(question, "学业风险", "预警", "风险学生", "毕业风险")) {
            return "academic_risk";
        }
        if (isGradeTrendQuestion(question)) {
            return "grade_trend";
        }
        if (containsAny(question, "不及格课程", "挂科课程", "不及格分析", "挂科分析")) {
            return "failed_course";
        }
        if (containsAny(question, "考勤异常", "异常考勤", "缺勤异常", "迟到异常")) {
            return "attendance_abnormal";
        }
        if (containsAny(question, "课程负载", "课程容量", "选课人数", "选课", "容量", "负载")) {
            return "course_load";
        }
        if (containsAny(question, "考勤", "缺勤", "旷课", "迟到", "请假", "出勤")) {
            return "attendance";
        }
        if (containsAny(question, "成绩", "挂科", "不及格", "及格率", "平均分", "分数")) {
            return "grade";
        }
        return "overview";
    }

    private IntentResolution resolveIntentWithFallback(String question) {
        if (llmClient != null) {
            try {
                String intent = normalize(llmClient.recognizeIntent(question));
                if (isSupportedIntent(intent)) {
                    return new IntentResolution(intent, "deepseek");
                }
            } catch (RuntimeException ignored) {
                // Keep the existing keyword matching path available when DeepSeek is unavailable.
            }
        }
        return new IntentResolution(resolveIntent(question), "keyword");
    }

    private boolean isGradeTrendQuestion(String question) {
        return containsAny(question, "成绩趋势", "成绩变化", "分数趋势", "成绩走势");
    }

    private boolean isMajorSummaryQuestion(String question) {
        return containsAny(question, "各专业", "专业学生", "专业人数", "每个专业")
                && containsAny(question, "学生", "人数", "多少", "规模");
    }

    private String buildMajorSummaryExplanation(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "当前筛选范围暂无专业学生数据。";
        }
        StringBuilder answer = new StringBuilder("当前筛选范围：");
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            if (index > 0) {
                answer.append("；");
            }
            answer.append(row.get("majorName")).append(" ")
                    .append(row.get("studentCount")).append(" 人")
                    .append("（预警率 ").append(row.get("warningRate")).append("%）");
        }
        return answer.append("。").toString();
    }

    private AnswerResolution explainWithFallback(String question, String intent, Map<String, Object> metrics,
                                                 EducationMetricsOverviewDTO overview, String fallbackAnswer) {
        if (llmClient != null) {
            try {
                String explanation = normalize(llmClient.explain(question, intent, metrics, overview));
                if (isSafeLlmExplanation(explanation)) {
                    return new AnswerResolution(explanation, "deepseek");
                }
            } catch (RuntimeException ignored) {
                // The deterministic metric explanation remains the user-visible fallback.
            }
        }
        return new AnswerResolution(fallbackAnswer, "keyword");
    }

    private static class IntentResolution {
        private final String intent;
        private final String source;

        private IntentResolution(String intent, String source) {
            this.intent = intent;
            this.source = source;
        }
    }

    private static class AnswerResolution {
        private final String answer;
        private final String source;

        private AnswerResolution(String answer, String source) {
            this.answer = answer;
            this.source = source;
        }
    }

    private boolean isSafeLlmExplanation(String explanation) {
        if (explanation == null || explanation.length() > 500) {
            return false;
        }
        String normalized = explanation.toLowerCase(Locale.ROOT);
        return !(normalized.contains("```sql")
                || normalized.contains("select ")
                || normalized.contains("insert ")
                || normalized.contains("update ")
                || normalized.contains("delete ")
                || normalized.contains("drop ")
                || normalized.contains("alter ")
                || normalized.contains("create table"));
    }

    private boolean isSupportedIntent(String intent) {
        return "overview".equals(intent)
                || "grade".equals(intent)
                || "attendance".equals(intent)
                || "course_load".equals(intent)
                || "academic_risk".equals(intent)
                || "grade_trend".equals(intent)
                || "failed_course".equals(intent)
                || "attendance_abnormal".equals(intent)
                || "student_comparison".equals(intent);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<Student> findStudents(String departmentId, String majorId, String classId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (normalize(departmentId) != null) {
            wrapper.eq(Student::getDepartmentId, normalize(departmentId));
        }
        if (normalize(majorId) != null) {
            wrapper.eq(Student::getMajorId, normalize(majorId));
        }
        if (normalize(classId) != null) {
            wrapper.eq(Student::getClassId, normalize(classId));
        }
        return studentMapper.selectList(wrapper);
    }

    private List<String> findStudentIds(String departmentId, String majorId, String classId) {
        if (!hasStudentScope(departmentId, majorId, classId)) {
            return Collections.emptyList();
        }
        List<Student> students = findStudents(departmentId, majorId, classId);
        List<String> ids = new ArrayList<>();
        for (Student student : students) {
            ids.add(student.getStudentId());
        }
        return ids;
    }

    private Map<String, Integer> countSelections(Set<String> scheduleIds, String semesterId, String departmentId,
                                                  String majorId, String classId) {
        if (scheduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<StudentCourseSelection> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(StudentCourseSelection::getScheduleId, scheduleIds)
                .eq(StudentCourseSelection::getStatus, "selected");
        if (normalize(semesterId) != null) {
            wrapper.eq(StudentCourseSelection::getSemesterId, normalize(semesterId));
        }
        List<String> studentIds = findStudentIds(departmentId, majorId, classId);
        if (hasStudentScope(departmentId, majorId, classId)) {
            if (studentIds.isEmpty()) {
                return Collections.emptyMap();
            }
            wrapper.in(StudentCourseSelection::getStudentId, studentIds);
        }
        List<StudentCourseSelection> selections = selectionMapper.selectList(wrapper);
        Map<String, Integer> counts = new HashMap<>();
        for (StudentCourseSelection selection : selections) {
            counts.put(selection.getScheduleId(), counts.getOrDefault(selection.getScheduleId(), 0) + 1);
        }
        return counts;
    }

    private Map<String, Course> findCourses(Set<String> courseIds) {
        if (courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Course> result = new HashMap<>();
        for (Course course : courseMapper.selectBatchIds(courseIds)) {
            result.put(course.getCourseId(), course);
        }
        return result;
    }

    private void applyGradeScope(LambdaQueryWrapper<Grade> wrapper, List<String> studentIds, boolean scoped) {
        if (!scoped) {
            return;
        }
        if (studentIds.isEmpty()) {
            wrapper.eq(Grade::getStudentId, "__NO_STUDENT__");
        } else {
            wrapper.in(Grade::getStudentId, studentIds);
        }
    }

    private void applyAttendanceScope(LambdaQueryWrapper<Attendance> wrapper, List<String> studentIds, boolean scoped) {
        if (!scoped) {
            return;
        }
        if (studentIds.isEmpty()) {
            wrapper.eq(Attendance::getStudentId, "__NO_STUDENT__");
        } else {
            wrapper.in(Attendance::getStudentId, studentIds);
        }
    }

    private boolean hasStudentScope(String departmentId, String majorId, String classId) {
        return normalize(departmentId) != null || normalize(majorId) != null || normalize(classId) != null;
    }

    private Map<String, Object> toOverviewMap(EducationMetricsOverviewDTO overview) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentCount", overview.getStudentCount());
        result.put("activeStudentCount", overview.getActiveStudentCount());
        result.put("gradeRecordCount", overview.getGradeRecordCount());
        result.put("failedCourseCount", overview.getFailedCourseCount());
        result.put("gradePassRate", overview.getGradePassRate());
        result.put("attendanceRecordCount", overview.getAttendanceRecordCount());
        result.put("abnormalAttendanceCount", overview.getAbnormalAttendanceCount());
        result.put("graduationAuditCount", overview.getGraduationAuditCount());
        result.put("approvedGraduationCount", overview.getApprovedGraduationCount());
        result.put("rejectedGraduationCount", overview.getRejectedGraduationCount());
        result.put("graduationApprovalRate", overview.getGraduationApprovalRate());
        return result;
    }

    private Map<String, Object> buildRiskSignal(EducationMetricsOverviewDTO overview) {
        Map<String, Object> signal = new LinkedHashMap<>();
        boolean attention = overview.getFailedCourseCount() > 0 || overview.getAbnormalAttendanceCount() > 0;
        signal.put("level", attention ? "attention" : "normal");
        signal.put("message", attention
                ? "发现挂科或异常考勤记录，建议进入学业预警 Agent 继续定位学生。"
                : "当前筛选范围暂无明显风险信号。");
        return signal;
    }

    private List<String> buildInsights(EducationMetricsOverviewDTO overview, Map<String, Object> courseLoadMetrics) {
        List<String> insights = new ArrayList<>();
        insights.add("当前范围学生数 " + overview.getStudentCount() + " 人，成绩通过率 " + overview.getGradePassRate() + "% 。");
        if (overview.getFailedCourseCount() > 0) {
            insights.add("发现 " + overview.getFailedCourseCount() + " 条已审核不及格成绩，建议关注对应学生。"
            );
        }
        if (overview.getAbnormalAttendanceCount() > 0) {
            insights.add("发现 " + overview.getAbnormalAttendanceCount() + " 条异常考勤记录，可与成绩风险联合判断。"
            );
        }
        insights.add("课程选课负载率为 " + courseLoadMetrics.get("loadRate") + "% 。");
        return insights;
    }

    private List<Map<String, Object>> buildModules() {
        List<Map<String, Object>> modules = new ArrayList<>();
        modules.add(module("overview", "教务概览", "学生、成绩、考勤和毕业审核核心指标"));
        modules.add(module("grade", "成绩分析", "平均分、通过率、成绩分布和课程均分"));
        modules.add(module("attendance", "考勤分析", "异常考勤数量、比例和学生覆盖数"));
        modules.add(module("course_load", "课程负载", "排课容量、选课人数和选课负载率"));
        return modules;
    }

    private Map<String, Object> module(String code, String name, String description) {
        Map<String, Object> module = new LinkedHashMap<>();
        module.put("code", code);
        module.put("name", name);
        module.put("description", description);
        return module;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listValue(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.<Map<String, Object>>emptyList();
    }

    private long numberValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    private double doubleValue(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }

    private double percentValue(Object value) {
        if (value == null) {
            return 0;
        }
        String text = value.toString().replace("%", "");
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
