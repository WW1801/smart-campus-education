package com.campus.education.service;

import com.campus.education.common.BusinessException;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.GradeMapper;
import com.campus.education.mapper.ScheduleMapper;
import com.campus.education.mapper.StudentCourseSelectionMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonnelLifecycleServiceTest {
    @Test
    void shouldBlockStudentDeletionWhenHistoryExists() {
        UserService users = mock(UserService.class);
        AttendanceMapper attendance = mock(AttendanceMapper.class);
        GradeMapper grades = mock(GradeMapper.class);
        ScheduleMapper schedules = mock(ScheduleMapper.class);
        StudentCourseSelectionMapper selections = mock(StudentCourseSelectionMapper.class);
        when(attendance.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> new PersonnelLifecycleService(users, attendance, grades, schedules, selections)
                .verifyStudentCanDelete("S001"));
    }

    @Test
    void shouldAllowDeletionWhenNoReferencesExist() {
        UserService users = mock(UserService.class);
        AttendanceMapper attendance = mock(AttendanceMapper.class);
        GradeMapper grades = mock(GradeMapper.class);
        ScheduleMapper schedules = mock(ScheduleMapper.class);
        StudentCourseSelectionMapper selections = mock(StudentCourseSelectionMapper.class);
        when(attendance.selectCount(any())).thenReturn(0L);
        when(grades.selectCount(any())).thenReturn(0L);
        when(selections.selectCount(any())).thenReturn(0L);
        when(users.count(any())).thenReturn(0L);
        assertDoesNotThrow(() -> new PersonnelLifecycleService(users, attendance, grades, schedules, selections)
                .verifyStudentCanDelete("S001"));
    }
}
