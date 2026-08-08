package com.campus.education.service.impl;

import com.campus.education.entity.Semester;
import com.campus.education.mapper.SemesterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentCourseSelectionSemesterResolutionTest {
    private StudentCourseSelectionServiceImpl service;
    private SemesterMapper semesterMapper;

    @BeforeEach
    void setUp() {
        service = new StudentCourseSelectionServiceImpl();
        semesterMapper = mock(SemesterMapper.class);
        ReflectionTestUtils.setField(service, "semesterMapper", semesterMapper);
    }

    @Test
    void resolvesCurrentSemesterByDateRangeInsteadOfStoredStatus() {
        Semester current = new Semester();
        current.setSemesterId("SEM-NOW");
        current.setStatus("completed");
        when(semesterMapper.selectOne(any())).thenReturn(current);

        String resolved = ReflectionTestUtils.invokeMethod(service, "resolveSemesterId", "");

        assertEquals("SEM-NOW", resolved);
        verify(semesterMapper).selectOne(any());
    }
}
