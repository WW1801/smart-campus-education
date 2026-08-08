package com.campus.education.common;

import com.campus.education.entity.Student;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentNoResolverTest {

    @Test
    void shouldFillMissingAndLegacyStudentNosInStableOrder() {
        Student first = student("S001", null, LocalDateTime.of(2021, 9, 1, 8, 0));
        Student second = student("S002", "2021001001001001", LocalDateTime.of(2021, 9, 2, 8, 0));

        StudentNoResolver.fillDisplayStudentNos(Arrays.asList(second, first));

        assertEquals("2021001001001", first.getStudentNo());
        assertEquals("2021001001002", second.getStudentNo());
        assertTrue(StudentNoResolver.isStandard(first));
        assertTrue(StudentNoResolver.isStandard(second));
    }

    @Test
    void shouldKeepExistingStandardNumberAndUseNextAvailableSequence() {
        Student existing = student("S001", "2021001001001", LocalDateTime.of(2021, 9, 1, 8, 0));
        Student missing = student("S002", null, LocalDateTime.of(2021, 9, 2, 8, 0));

        StudentNoResolver.fillDisplayStudentNos(Arrays.asList(existing, missing));

        assertEquals("2021001001001", existing.getStudentNo());
        assertEquals("2021001001002", missing.getStudentNo());
    }

    @Test
    void shouldAllocateConsecutiveNumbersAcrossDifferentClasses() {
        Student firstClass = student("S001", null, LocalDateTime.of(2021, 9, 1, 8, 0));
        firstClass.setClassId("C001");
        Student secondClass = student("S002", "2021001001001001", LocalDateTime.of(2021, 9, 2, 8, 0));
        secondClass.setClassId("C002");

        StudentNoResolver.fillDisplayStudentNos(Arrays.asList(secondClass, firstClass));

        assertEquals("2021001001001", firstClass.getStudentNo());
        assertEquals("2021001001002", secondClass.getStudentNo());
    }

    private Student student(String id, String studentNo, LocalDateTime createdAt) {
        Student student = new Student();
        student.setStudentId(id);
        student.setStudentNo(studentNo);
        student.setDepartmentId("D001");
        student.setMajorId("M001");
        student.setEnrollmentDate(LocalDate.of(2021, 9, 1));
        student.setCreatedAt(createdAt);
        return student;
    }
}
