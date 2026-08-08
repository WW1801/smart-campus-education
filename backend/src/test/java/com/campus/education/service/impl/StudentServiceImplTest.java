package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.entity.Student;
import com.campus.education.mapper.StudentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentServiceImplTest {

    private StudentServiceImpl studentService;
    private StudentMapper studentMapper;

    @BeforeEach
    void setUp() {
        studentService = new StudentServiceImpl();
        studentMapper = mock(StudentMapper.class);
        ReflectionTestUtils.setField(studentService, "baseMapper", studentMapper);
    }

    @Test
    void shouldGenerateStudentNoFromEnrollmentAndBusinessCodes() {
        when(studentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.<Student>emptyList());
        when(studentMapper.insert(any(Student.class))).thenReturn(1);
        Student student = student("D001", "M001", "C001", LocalDate.of(2026, 9, 1));

        studentService.createStudent(student);

        assertEquals("2026001001001", student.getStudentNo());
        assertEquals("active", student.getStatus());
        verify(studentMapper).insert(student);
    }

    @Test
    void shouldContinueSequenceWithinSameMajorAndYear() {
        Student existingInClass = student("D001", "M001", "C002", LocalDate.of(2026, 9, 1));
        existingInClass.setStudentNo("2026001001007");
        when(studentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(existingInClass));
        when(studentMapper.insert(any(Student.class))).thenReturn(1);
        Student student = student("D001", "M001", "C001", LocalDate.of(2026, 9, 1));

        studentService.createStudent(student);

        assertEquals("2026001001008", student.getStudentNo());
    }

    @Test
    void shouldRejectExistingDuplicateStandardNumberBeforeCreatingStudent() {
        Student first = student("D001", "M001", "C001", LocalDate.of(2026, 9, 1));
        first.setStudentNo("2026001001001");
        Student duplicate = student("D001", "M001", "C002", LocalDate.of(2026, 9, 1));
        duplicate.setStudentNo("2026001001001");
        when(studentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(first, duplicate));

        assertThrows(com.campus.education.common.BusinessException.class,
                () -> studentService.createStudent(student("D001", "M001", "C003", LocalDate.of(2026, 9, 1))));
    }

    @Test
    void shouldKeepGeneratedStudentNoWhenUpdatingStudentProfile() {
        Student stored = student("D001", "M001", "C001", LocalDate.of(2026, 9, 1));
        stored.setStudentId("internal-id");
        stored.setStudentNo("2026001001001");
        when(studentMapper.selectById("internal-id")).thenReturn(stored);
        when(studentMapper.updateById(any(Student.class))).thenReturn(1);
        Student edited = student("D002", "M002", "C002", LocalDate.of(2026, 9, 1));
        edited.setStudentId("internal-id");
        edited.setStudentNo("manual-change");

        studentService.updateStudent(edited);

        assertEquals("2026001001001", edited.getStudentNo());
        verify(studentMapper).updateById(edited);
    }

    private Student student(String departmentId, String majorId, String classId, LocalDate enrollmentDate) {
        Student student = new Student();
        student.setDepartmentId(departmentId);
        student.setMajorId(majorId);
        student.setClassId(classId);
        student.setEnrollmentDate(enrollmentDate);
        return student;
    }
}
