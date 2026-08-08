package com.campus.education.service.impl;

import com.campus.education.common.BusinessException;
import com.campus.education.entity.Grade;
import com.campus.education.mapper.GradeMapper;
import com.campus.education.mapper.TeachingPlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GradeServiceImplTest {
    private GradeServiceImpl service;
    private GradeMapper gradeMapper;

    @BeforeEach
    void setUp() {
        service = new GradeServiceImpl();
        gradeMapper = mock(GradeMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", gradeMapper);
        ReflectionTestUtils.setField(service, "teachingPlanMapper", mock(TeachingPlanMapper.class));
        when(gradeMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void submitCalculatesTotalAndPersistsSubmittedStatus() {
        Grade grade = grade();
        when(gradeMapper.insert(any(Grade.class))).thenReturn(1);

        service.submitGrade(grade);

        assertEquals("submitted", grade.getStatus());
        assertEquals(90.7, grade.getTotalScore());
        assertEquals(null, grade.getIsPass());
    }

    @Test
    void legacyStatusEnumReturnsActionableMigrationMessage() {
        Grade grade = grade();
        when(gradeMapper.insert(any(Grade.class))).thenThrow(new DataIntegrityViolationException(
                "Data truncated for column 'status' at row 1"));

        BusinessException error = assertThrows(BusinessException.class, () -> service.submitGrade(grade));

        assertTrue(error.getMessage().contains("20260726_grade_status.sql"));
    }

    private Grade grade() {
        Grade grade = new Grade();
        grade.setStudentId("S1");
        grade.setCourseId("C1");
        grade.setSemesterId("SEM1");
        grade.setTeacherId("T1");
        grade.setUsualScore(90.0);
        grade.setExamScore(91.0);
        return grade;
    }
}
