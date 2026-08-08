package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessException;
import com.campus.education.dto.agent.AcademicWarningRecordDTO;
import com.campus.education.entity.AcademicWarningRecord;
import com.campus.education.entity.Student;
import com.campus.education.mapper.AcademicWarningRecordMapper;
import com.campus.education.mapper.StudentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicWarningRecordServiceImplTest {

    private AcademicWarningRecordServiceImpl service;
    private AcademicWarningRecordMapper recordMapper;
    private StudentMapper studentMapper;

    @BeforeEach
    void setUp() {
        service = new AcademicWarningRecordServiceImpl();
        recordMapper = mock(AcademicWarningRecordMapper.class);
        studentMapper = mock(StudentMapper.class);
        ReflectionTestUtils.setField(service, "academicWarningRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
    }

    @Test
    void shouldReturnConvertedHistoryPageForExistingStudent() {
        when(studentMapper.selectById("S001")).thenReturn(student("S001", "测试学生"));
        Page<AcademicWarningRecord> source = new Page<>(1, 10, 1);
        source.setRecords(Collections.singletonList(record("R001", "S001", "[\"GRADE_FAILED\"]")));
        when(recordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(source);

        Page<AcademicWarningRecordDTO> result = (Page<AcademicWarningRecordDTO>) service.pageStudentHistory(
                " S001 ", null, null, null, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("S001", result.getRecords().get(0).getStudentId());
        assertEquals("测试学生", result.getRecords().get(0).getStudentName());
        assertEquals(Collections.singletonList("GRADE_FAILED"), result.getRecords().get(0).getTriggeredRules());
        verify(recordMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldReturnEmptyRecordsWhenStudentHasNoHistory() {
        when(studentMapper.selectById("S002")).thenReturn(student("S002", "空历史学生"));
        Page<AcademicWarningRecord> source = new Page<>(1, 10, 0);
        source.setRecords(Collections.<AcademicWarningRecord>emptyList());
        when(recordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(source);

        Page<AcademicWarningRecordDTO> result = (Page<AcademicWarningRecordDTO>) service.pageStudentHistory(
                "S002", null, null, null, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void shouldRejectHistoryQueryWhenStudentDoesNotExist() {
        when(studentMapper.selectById("S404")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.pageStudentHistory("S404", null, null, null, 1, 10));

        assertEquals("学生不存在", exception.getMessage());
    }

    @Test
    void shouldPropagateMapperFailureToUnifiedExceptionHandler() {
        when(studentMapper.selectById("S500")).thenReturn(student("S500", "异常学生"));
        when(recordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("数据库不可用"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.pageStudentHistory("S500", null, null, null, 1, 10));

        assertEquals("数据库不可用", exception.getMessage());
    }

    private Student student(String studentId, String name) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setName(name);
        return student;
    }

    private AcademicWarningRecord record(String recordId, String studentId, String triggeredRules) {
        AcademicWarningRecord record = new AcademicWarningRecord();
        record.setRecordId(recordId);
        record.setStudentId(studentId);
        record.setSemesterId("SEM001");
        record.setRiskLevel("medium");
        record.setRiskScore(25);
        record.setRiskReason("存在挂科");
        record.setTriggeredRules(triggeredRules);
        record.setCalculatedAt(LocalDateTime.of(2026, 7, 28, 10, 0));
        record.setProcessStatus("pending");
        return record;
    }
}
