package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.dto.GraduationAuditVO;
import com.campus.education.entity.Course;
import com.campus.education.entity.Grade;
import com.campus.education.entity.GraduationAudit;
import com.campus.education.entity.Student;
import com.campus.education.entity.TeachingPlan;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.GradeMapper;
import com.campus.education.mapper.GraduationAuditMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeachingPlanMapper;
import com.campus.education.service.GraduationAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GraduationAuditServiceImpl extends ServiceImpl<GraduationAuditMapper, GraduationAudit> implements GraduationAuditService {

    private static final double MIN_GPA_FOR_GRADUATION = 1.0;

    @Autowired
    private GraduationAuditMapper graduationAuditMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeachingPlanMapper teachingPlanMapper;

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional
    public GraduationAuditVO auditStudent(String studentId, String auditOpinion) {
        Student student = getStudent(studentId);
        GraduationAudit audit = buildOrUpdateAudit(student, auditOpinion);
        GraduationAudit existing = findByStudentId(studentId);
        if (existing == null) {
            graduationAuditMapper.insert(audit);
        } else {
            audit.setAuditId(existing.getAuditId());
            audit.setDegreeGranted(Boolean.TRUE.equals(existing.getDegreeGranted()));
            audit.setDegreeAwardTime(existing.getDegreeAwardTime());
            audit.setCertificateNo(existing.getCertificateNo());
            audit.setCreatedAt(existing.getCreatedAt());
            graduationAuditMapper.updateById(audit);
        }
        return toView(student, audit);
    }

    @Override
    public GraduationAuditVO getAuditDetail(String studentId) {
        Student student = getStudent(studentId);
        GraduationAudit audit = findByStudentId(studentId);
        return toView(student, audit);
    }

    @Override
    @Transactional
    public List<GraduationAuditVO> batchAudit(String majorId, String classId) {
        List<Student> students = listStudents(null, majorId, classId);
        List<GraduationAuditVO> result = new ArrayList<>();
        for (Student student : students) {
            result.add(auditStudent(student.getStudentId(), null));
        }
        return result;
    }

    @Override
    @Transactional
    public GraduationAuditVO grantDegree(String studentId, String auditOpinion) {
        Student student = getStudent(studentId);
        GraduationAudit audit = findByStudentId(studentId);
        if (audit == null) {
            throw new BusinessException("请先执行毕业审核");
        }
        if (!"approved".equals(audit.getStatus())) {
            throw new BusinessException("仅审核通过的学生可以授予学位");
        }

        audit.setDegreeGranted(true);
        audit.setDegreeAwardTime(LocalDateTime.now());
        audit.setCertificateNo(generateCertificateNo(studentId));
        if (auditOpinion != null && !auditOpinion.trim().isEmpty()) {
            audit.setAuditOpinion(auditOpinion.trim());
        }
        graduationAuditMapper.updateById(audit);

        if (!"graduated".equals(student.getStatus())) {
            student.setStatus("graduated");
            studentMapper.updateById(student);
        }
        return toView(student, audit);
    }

    @Override
    public IPage<GraduationAuditVO> pageAudits(Integer current, Integer size, String studentId, String majorId, String classId, String status) {
        List<Student> students = listStudents(studentId, majorId, classId);
        if (students.isEmpty()) {
            return new Page<>(current, size);
        }

        List<String> studentIds = students.stream().map(Student::getStudentId).collect(Collectors.toList());
        Map<String, GraduationAudit> auditMap = graduationAuditMapper.selectList(
                new LambdaQueryWrapper<GraduationAudit>().in(GraduationAudit::getStudentId, studentIds)
        ).stream().collect(Collectors.toMap(GraduationAudit::getStudentId, item -> item));

        List<GraduationAuditVO> records = students.stream()
                .map(student -> toView(student, auditMap.get(student.getStudentId())))
                .filter(item -> status == null || status.trim().isEmpty() || status.equals(item.getStatus()))
                .sorted(Comparator.comparing(GraduationAuditVO::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(GraduationAuditVO::getStudentId))
                .collect(Collectors.toList());

        Page<GraduationAuditVO> page = new Page<>(current, size);
        page.setTotal(records.size());
        long fromIndex = Math.max(0, (long) (current - 1) * size);
        long toIndex = Math.min(records.size(), fromIndex + size);
        if (fromIndex >= records.size()) {
            page.setRecords(Collections.emptyList());
        } else {
            page.setRecords(records.subList((int) fromIndex, (int) toIndex));
        }
        return page;
    }

    @Override
    public Map<String, Object> getStatistics(String majorId, String classId) {
        List<Student> students = listStudents(null, majorId, classId);
        List<GraduationAuditVO> views = students.stream()
                .map(student -> toView(student, findByStudentId(student.getStudentId())))
                .collect(Collectors.toList());

        long auditedCount = views.stream().filter(item -> !"unaudited".equals(item.getStatus())).count();
        long approvedCount = views.stream().filter(item -> "approved".equals(item.getStatus())).count();
        long rejectedCount = views.stream().filter(item -> "rejected".equals(item.getStatus())).count();
        long degreeGrantedCount = views.stream().filter(item -> Boolean.TRUE.equals(item.getDegreeGranted())).count();
        long pendingDegreeCount = views.stream()
                .filter(item -> "approved".equals(item.getStatus()) && !Boolean.TRUE.equals(item.getDegreeGranted()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStudents", views.size());
        result.put("auditedCount", auditedCount);
        result.put("approvedCount", approvedCount);
        result.put("rejectedCount", rejectedCount);
        result.put("degreeGrantedCount", degreeGrantedCount);
        result.put("pendingDegreeCount", pendingDegreeCount);
        result.put("approvalRate", views.isEmpty() ? "0.00%" : formatPercent(approvedCount, views.size()));
        result.put("degreeRate", views.isEmpty() ? "0.00%" : formatPercent(degreeGrantedCount, views.size()));
        return result;
    }

    private GraduationAudit buildOrUpdateAudit(Student student, String auditOpinion) {
        List<TeachingPlan> plans = teachingPlanMapper.selectList(
                new LambdaQueryWrapper<TeachingPlan>().eq(TeachingPlan::getMajorId, student.getMajorId())
        );
        List<Grade> approvedGrades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, student.getStudentId())
                        .eq(Grade::getStatus, "approved")
        );

        Map<String, Course> courseMap = loadCourseMap(approvedGrades, plans);
        Map<String, Grade> bestGradeMap = pickBestGradeByCourse(approvedGrades);
        Set<String> passedCourseIds = bestGradeMap.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsPass()))
                .map(Grade::getCourseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        double totalCredits = passedCourseIds.stream()
                .map(courseMap::get)
                .filter(Objects::nonNull)
                .mapToDouble(Course::getCredits)
                .sum();

        Map<String, TeachingPlan> planByCourseId = plans.stream()
                .collect(Collectors.toMap(TeachingPlan::getCourseId, item -> item, (a, b) -> a));

        double requiredCredits = planByCourseId.keySet().stream()
                .map(courseMap::get)
                .filter(Objects::nonNull)
                .mapToDouble(Course::getCredits)
                .sum();

        double electivePublicCredits = passedCourseIds.stream()
                .filter(planByCourseId::containsKey)
                .filter(courseId -> "elective_public".equals(planByCourseId.get(courseId).getCourseNature()))
                .map(courseMap::get)
                .filter(Objects::nonNull)
                .mapToDouble(Course::getCredits)
                .sum();

        double electiveMajorCredits = passedCourseIds.stream()
                .filter(planByCourseId::containsKey)
                .filter(courseId -> "elective_major".equals(planByCourseId.get(courseId).getCourseNature()))
                .map(courseMap::get)
                .filter(Objects::nonNull)
                .mapToDouble(Course::getCredits)
                .sum();

        boolean compulsoryPass = plans.stream()
                .filter(item -> "compulsory".equals(item.getCourseNature()))
                .map(TeachingPlan::getCourseId)
                .allMatch(passedCourseIds::contains);

        double gpa = calculateGpa(bestGradeMap, courseMap);
        boolean approved = !plans.isEmpty()
                && totalCredits >= requiredCredits
                && compulsoryPass
                && gpa >= MIN_GPA_FOR_GRADUATION;

        GraduationAudit audit = new GraduationAudit();
        audit.setStudentId(student.getStudentId());
        audit.setTotalCredits(round(totalCredits));
        audit.setRequiredCredits(round(requiredCredits));
        audit.setCompulsoryPass(compulsoryPass);
        audit.setElectivePublicCredits(round(electivePublicCredits));
        audit.setElectiveMajorCredits(round(electiveMajorCredits));
        audit.setGpa(round(gpa));
        audit.setStatus(approved ? "approved" : "rejected");
        audit.setAuditOpinion(resolveOpinion(plans, totalCredits, requiredCredits, compulsoryPass, gpa, auditOpinion));
        audit.setUpdatedAt(LocalDateTime.now());
        if (audit.getCreatedAt() == null) {
            audit.setCreatedAt(LocalDateTime.now());
        }
        return audit;
    }

    private GraduationAuditVO toView(Student student, GraduationAudit audit) {
        GraduationAuditVO view = new GraduationAuditVO();
        view.setStudentId(student.getStudentId());
        view.setStudentName(student.getName());
        view.setMajorId(student.getMajorId());
        view.setClassId(student.getClassId());

        if (audit == null) {
            view.setStatus("unaudited");
            view.setAuditOpinion("未执行毕业审核");
            view.setDegreeGranted(false);
            return view;
        }

        view.setAuditId(audit.getAuditId());
        view.setTotalCredits(audit.getTotalCredits());
        view.setRequiredCredits(audit.getRequiredCredits());
        view.setCompulsoryPass(audit.getCompulsoryPass());
        view.setElectivePublicCredits(audit.getElectivePublicCredits());
        view.setElectiveMajorCredits(audit.getElectiveMajorCredits());
        view.setGpa(audit.getGpa());
        view.setStatus(audit.getStatus());
        view.setAuditOpinion(audit.getAuditOpinion());
        view.setDegreeGranted(Boolean.TRUE.equals(audit.getDegreeGranted()));
        view.setDegreeAwardTime(audit.getDegreeAwardTime());
        view.setCertificateNo(audit.getCertificateNo());
        view.setUpdatedAt(audit.getUpdatedAt());
        return view;
    }

    private Student getStudent(String studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        return student;
    }

    private GraduationAudit findByStudentId(String studentId) {
        return graduationAuditMapper.selectOne(
                new LambdaQueryWrapper<GraduationAudit>().eq(GraduationAudit::getStudentId, studentId).last("LIMIT 1")
        );
    }

    private List<Student> listStudents(String studentId, String majorId, String classId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.eq(Student::getStudentId, studentId.trim());
        }
        if (majorId != null && !majorId.trim().isEmpty()) {
            wrapper.eq(Student::getMajorId, majorId.trim());
        }
        if (classId != null && !classId.trim().isEmpty()) {
            wrapper.eq(Student::getClassId, classId.trim());
        }
        wrapper.orderByAsc(Student::getStudentId);
        return studentMapper.selectList(wrapper);
    }

    private Map<String, Course> loadCourseMap(List<Grade> grades, List<TeachingPlan> plans) {
        Set<String> courseIds = new LinkedHashSet<>();
        for (Grade grade : grades) {
            courseIds.add(grade.getCourseId());
        }
        for (TeachingPlan plan : plans) {
            courseIds.add(plan.getCourseId());
        }
        if (courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, item -> item));
    }

    private Map<String, Grade> pickBestGradeByCourse(List<Grade> grades) {
        Map<String, Grade> result = new LinkedHashMap<>();
        for (Grade grade : grades) {
            Grade existing = result.get(grade.getCourseId());
            if (existing == null || existing.getTotalScore() == null || grade.getTotalScore() > existing.getTotalScore()) {
                result.put(grade.getCourseId(), grade);
            }
        }
        return result;
    }

    private double calculateGpa(Map<String, Grade> bestGradeMap, Map<String, Course> courseMap) {
        double totalWeightedPoints = 0.0;
        double totalCredits = 0.0;
        for (Grade grade : bestGradeMap.values()) {
            Course course = courseMap.get(grade.getCourseId());
            if (course == null || grade.getTotalScore() == null) {
                continue;
            }
            double credit = course.getCredits();
            double gradePoint = grade.getTotalScore() >= 60 ? (grade.getTotalScore() - 50) / 10 : 0;
            totalWeightedPoints += gradePoint * credit;
            totalCredits += credit;
        }
        return totalCredits == 0 ? 0 : totalWeightedPoints / totalCredits;
    }

    private String resolveOpinion(List<TeachingPlan> plans, double totalCredits, double requiredCredits, boolean compulsoryPass,
                                  double gpa, String manualOpinion) {
        if (manualOpinion != null && !manualOpinion.trim().isEmpty()) {
            return manualOpinion.trim();
        }
        if (plans.isEmpty()) {
            return "未配置教学计划，暂无法完成毕业审核";
        }

        List<String> reasons = new ArrayList<>();
        if (totalCredits < requiredCredits) {
            reasons.add("学分未达标");
        }
        if (!compulsoryPass) {
            reasons.add("必修课未全部通过");
        }
        if (gpa < MIN_GPA_FOR_GRADUATION) {
            reasons.add("绩点未达标");
        }
        if (reasons.isEmpty()) {
            return "满足毕业条件，可授予学位";
        }
        return String.join("，", reasons);
    }

    private String generateCertificateNo(String studentId) {
        return "DEG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + studentId;
    }

    private String formatPercent(long numerator, int denominator) {
        return String.format("%.2f%%", denominator == 0 ? 0 : numerator * 100.0 / denominator);
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
