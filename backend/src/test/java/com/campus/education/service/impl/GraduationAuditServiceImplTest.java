package com.campus.education.service.impl;

/**
 * GraduationAuditServiceImpl测试类，负责验证GraduationAuditServiceImpl相关逻辑。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraduationAuditServiceImplTest {

    private GraduationAuditServiceImpl service;
    private GraduationAuditMapper graduationAuditMapper;
    private StudentMapper studentMapper;
    private TeachingPlanMapper teachingPlanMapper;
    private GradeMapper gradeMapper;
    private CourseMapper courseMapper;

    @BeforeEach
    void setUp() {
        service = new GraduationAuditServiceImpl();
        graduationAuditMapper = mock(GraduationAuditMapper.class);
        studentMapper = mock(StudentMapper.class);
        teachingPlanMapper = mock(TeachingPlanMapper.class);
        gradeMapper = mock(GradeMapper.class);
        courseMapper = mock(CourseMapper.class);

        ReflectionTestUtils.setField(service, "graduationAuditMapper", graduationAuditMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "teachingPlanMapper", teachingPlanMapper);
        ReflectionTestUtils.setField(service, "gradeMapper", gradeMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "baseMapper", graduationAuditMapper);
    }

    @Test
    void shouldApproveGraduationWhenCreditsAndCompulsoryCoursesMeetRequirements() {
        Student student = buildStudent("S100", "M100", "C100");
        Course course1 = buildCourse("CO1", 3.0);
        Course course2 = buildCourse("CO2", 2.0);
        TeachingPlan plan1 = buildPlan("CO1", "compulsory");
        TeachingPlan plan2 = buildPlan("CO2", "elective_major");
        Grade grade1 = buildGrade("CO1", 88.0, true);
        Grade grade2 = buildGrade("CO2", 92.0, true);

        when(studentMapper.selectById("S100")).thenReturn(student);
        when(teachingPlanMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<TeachingPlan>>any()))
                .thenReturn(Arrays.asList(plan1, plan2));
        when(gradeMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Grade>>any()))
                .thenReturn(Arrays.asList(grade1, grade2));
        when(courseMapper.selectBatchIds(ArgumentMatchers.anyCollection()))
                .thenReturn(Arrays.asList(course1, course2));
        when(graduationAuditMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<GraduationAudit>>any()))
                .thenReturn(null);
        when(graduationAuditMapper.insert(any(GraduationAudit.class))).thenReturn(1);

        GraduationAuditVO result = service.auditStudent("S100", null);

        assertEquals("approved", result.getStatus());
        assertTrue(Boolean.TRUE.equals(result.getCompulsoryPass()));
        assertEquals(5.0, result.getRequiredCredits());
        assertEquals(5.0, result.getTotalCredits());
        verify(graduationAuditMapper).insert(any(GraduationAudit.class));
    }

    @Test
    void shouldRejectGraduationWhenCompulsoryCourseNotPassed() {
        Student student = buildStudent("S101", "M100", "C100");
        Course course1 = buildCourse("CO1", 3.0);
        TeachingPlan plan1 = buildPlan("CO1", "compulsory");
        Grade failedGrade = buildGrade("CO1", 58.0, false);

        when(studentMapper.selectById("S101")).thenReturn(student);
        when(teachingPlanMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<TeachingPlan>>any()))
                .thenReturn(Collections.singletonList(plan1));
        when(gradeMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<Grade>>any()))
                .thenReturn(Collections.singletonList(failedGrade));
        when(courseMapper.selectBatchIds(ArgumentMatchers.anyCollection()))
                .thenReturn(Collections.singletonList(course1));
        when(graduationAuditMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<GraduationAudit>>any()))
                .thenReturn(null);
        when(graduationAuditMapper.insert(any(GraduationAudit.class))).thenReturn(1);

        GraduationAuditVO result = service.auditStudent("S101", null);

        assertEquals("rejected", result.getStatus());
        assertFalse(Boolean.TRUE.equals(result.getCompulsoryPass()));
        assertTrue(result.getAuditOpinion().contains("必修课未全部通过"));
    }

    private Student buildStudent(String studentId, String majorId, String classId) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setName("测试学生");
        student.setMajorId(majorId);
        student.setClassId(classId);
        student.setStatus("active");
        return student;
    }

    private TeachingPlan buildPlan(String courseId, String nature) {
        TeachingPlan plan = new TeachingPlan();
        plan.setCourseId(courseId);
        plan.setCourseNature(nature);
        return plan;
    }

    private Course buildCourse(String courseId, double credits) {
        Course course = new Course();
        course.setCourseId(courseId);
        course.setCredits(credits);
        return course;
    }

    private Grade buildGrade(String courseId, double totalScore, boolean isPass) {
        Grade grade = new Grade();
        grade.setCourseId(courseId);
        grade.setTotalScore(totalScore);
        grade.setIsPass(isPass);
        return grade;
    }
}
