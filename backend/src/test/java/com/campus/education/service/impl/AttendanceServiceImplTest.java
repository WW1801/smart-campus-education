package com.campus.education.service.impl;

import com.campus.education.dto.attendance.BatchAttendanceRequest;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.Course;
import com.campus.education.entity.Semester;
import com.campus.education.entity.Student;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.CourseScheduleMapper;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.CourseRosterService;
import com.campus.education.service.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceServiceImplTest {
    private AttendanceServiceImpl service;
    private AttendanceMapper attendanceMapper;
    private StudentMapper studentMapper;
    private CourseMapper courseMapper;
    private SemesterMapper semesterMapper;
    private CourseScheduleMapper courseScheduleMapper;
    private CourseRosterService courseRosterService;

    @BeforeEach
    void setUp() {
        service = new AttendanceServiceImpl();
        attendanceMapper = mock(AttendanceMapper.class);
        studentMapper = mock(StudentMapper.class);
        courseMapper = mock(CourseMapper.class);
        semesterMapper = mock(SemesterMapper.class);
        courseScheduleMapper = mock(CourseScheduleMapper.class);
        courseRosterService = mock(CourseRosterService.class);
        ReflectionTestUtils.setField(service, "baseMapper", attendanceMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "semesterMapper", semesterMapper);
        ReflectionTestUtils.setField(service, "courseScheduleMapper", courseScheduleMapper);
        ReflectionTestUtils.setField(service, "courseRosterService", courseRosterService);

        when(studentMapper.selectById(any())).thenAnswer(invocation -> { Object id = invocation.getArgument(0); return student(String.valueOf(id)); });
        when(courseMapper.selectById("CO1")).thenReturn(new Course());
        when(semesterMapper.selectById("SEM1")).thenReturn(semester());
        when(courseScheduleMapper.selectCount(any())).thenReturn(1L);
        when(courseRosterService.listActiveStudents(any(), any(), any())).thenReturn(Arrays.asList(student("S1"), student("S2")));
        when(attendanceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(attendanceMapper.selectOne(any())).thenReturn(null);
        when(attendanceMapper.insert(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance value = invocation.getArgument(0); value.setAttendanceId("A-" + value.getStudentId()); return 1;
        });
    }

    @Test
    void shouldInsertNewAttendance() {
        Map<String, Object> result = service.batchSave(request(row("S1", "present")), null);
        assertEquals(1, result.get("successCount"));
        assertEquals("inserted", firstSuccess(result).get("operation"));
        assertEquals("学生S1", firstSuccess(result).get("studentName"));
        assertEquals("2026001001001", firstSuccess(result).get("studentNo"));
    }

    @Test
    void shouldUpdateExistingAttendance() {
        Attendance existing = attendance("A1", "S1", "absent");
        when(attendanceMapper.selectOne(any())).thenReturn(existing);
        Map<String, Object> result = service.batchSave(request(row("S1", "present")), null);
        assertEquals("updated", firstSuccess(result).get("operation"));
        assertEquals("present", existing.getStatus());
        verify(attendanceMapper).updateById(existing);
    }

    @Test
    void shouldNotOverwriteAttendanceSyncedFromApprovedLeave() {
        Attendance existing = attendance("A1", "S1", "leave");
        existing.setRecordSource("leave_request");
        existing.setSourceRequestId("LR1");
        when(attendanceMapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> result = service.batchSave(request(row("S1", "present")), null);

        assertEquals(0, result.get("successCount"));
        assertEquals("attendanceId", firstFailure(result).get("field"));
        assertEquals("该考勤由已审批请假申请同步生成，不能人工覆盖", firstFailure(result).get("reason"));
    }

    @Test
    void shouldNotOverwriteManuallyLockedAttendance() {
        Attendance existing = attendance("A1", "S1", "present");
        existing.setManualLocked(true);
        when(attendanceMapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> result = service.batchSave(request(row("S1", "late")), null);

        assertEquals(1, result.get("failedCount"));
        assertEquals("该考勤记录已被人工锁定", firstFailure(result).get("reason"));
    }

    @Test
    void shouldBeIdempotentOnRepeatedSubmission() {
        AtomicReference<Attendance> stored = new AtomicReference<>();
        when(attendanceMapper.selectOne(any())).thenAnswer(invocation -> stored.get());
        when(attendanceMapper.insert(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance value = invocation.getArgument(0); value.setAttendanceId("A1"); stored.set(value); return 1;
        });
        Map<String, Object> first = service.batchSave(request(row("S1", "present")), null);
        Map<String, Object> second = service.batchSave(request(row("S1", "late")), null);
        assertEquals("inserted", firstSuccess(first).get("operation"));
        assertEquals("updated", firstSuccess(second).get("operation"));
        assertEquals("late", stored.get().getStatus());
    }

    @Test
    void shouldReturnInvalidStudentCourseSemesterAndStatusDetails() {
        when(studentMapper.selectById("UNKNOWN")).thenReturn(null);
        when(courseMapper.selectById("BAD-COURSE")).thenReturn(null);
        BatchAttendanceRequest.Record badCourse = row("S1", "present"); badCourse.setCourseId("BAD-COURSE");
        BatchAttendanceRequest.Record badSemester = row("S1", "present"); badSemester.setSemesterId("BAD-SEM");
        Map<String, Object> result = service.batchSave(request(
                row("UNKNOWN", "present"), badCourse, badSemester, row("S2", "invalid")), null);
        assertEquals(0, result.get("successCount"));
        assertEquals(4, result.get("failedCount"));
        List<?> details = (List<?>) result.get("failureDetails");
        assertEquals("studentId", ((Map<?, ?>) details.get(0)).get("field"));
        assertEquals("courseId", ((Map<?, ?>) details.get(1)).get("field"));
        assertEquals("semesterId", ((Map<?, ?>) details.get(2)).get("field"));
        assertEquals("status", ((Map<?, ?>) details.get(3)).get("field"));
        assertNotNull(((Map<?, ?>) details.get(0)).get("suggestion"));
    }

    @Test
    void shouldRejectTeacherWithoutCoursePermission() {
        when(courseScheduleMapper.selectCount(any())).thenReturn(0L);
        Map<String, Object> result = service.batchSave(request(row("S1", "present")), "T9");
        Map<?, ?> failure = firstFailure(result);
        assertEquals("teacherId", failure.get("field"));
        assertEquals("当前教师没有该课程在本学期的授课权限", failure.get("reason"));
    }

    @Test
    void shouldCommitValidRowsAndReportBusinessFailures() {
        Map<String, Object> result = service.batchSave(request(row("S1", "present"), row("S2", "invalid")), null);
        assertEquals(1, result.get("successCount"));
        assertEquals(1, result.get("failedCount"));
        assertEquals("学生S2", firstFailure(result).get("studentName"));
    }

    @Test
    void shouldPropagateSystemExceptionForTransactionRollback() throws Exception {
        when(attendanceMapper.insert(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance value = invocation.getArgument(0);
            if ("S2".equals(value.getStudentId())) throw new IllegalStateException("database unavailable");
            value.setAttendanceId("A1");
            return 1;
        });
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        AttendanceService transactionalService = (AttendanceService) proxyFactory.getProxy();

        assertThrows(IllegalStateException.class,
                () -> transactionalService.batchSave(request(row("S1", "present"), row("S2", "late")), null));
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void shouldRejectDateOutsideSemester() {
        BatchAttendanceRequest request = request(row("S1", "present"));
        request.setDate(LocalDate.of(2027, 1, 1));
        Map<?, ?> failure = firstFailure(service.batchSave(request, null));
        assertEquals("date", failure.get("field"));
    }

    private Map<?, ?> firstSuccess(Map<String, Object> result) { return (Map<?, ?>) ((List<?>) result.get("successDetails")).get(0); }
    private Map<?, ?> firstFailure(Map<String, Object> result) { return (Map<?, ?>) ((List<?>) result.get("failureDetails")).get(0); }

    private BatchAttendanceRequest request(BatchAttendanceRequest.Record... rows) {
        BatchAttendanceRequest request = new BatchAttendanceRequest();
        request.setCourseId("CO1"); request.setSemesterId("SEM1"); request.setDate(LocalDate.of(2026, 3, 1));
        request.setRecords(Arrays.asList(rows)); return request;
    }

    private BatchAttendanceRequest.Record row(String studentId, String status) {
        BatchAttendanceRequest.Record row = new BatchAttendanceRequest.Record(); row.setStudentId(studentId); row.setStatus(status); return row;
    }

    private Student student(String id) { Student value = new Student(); value.setStudentId(id); value.setStudentNo("202600100100" + ("S2".equals(id) ? "2" : "1")); value.setName("学生" + id); value.setStatus("active"); return value; }
    private Semester semester() { Semester value = new Semester(); value.setStartDate(LocalDate.of(2026, 2, 1)); value.setEndDate(LocalDate.of(2026, 7, 31)); return value; }
    private Attendance attendance(String id, String studentId, String status) {
        Attendance value = new Attendance(); value.setAttendanceId(id); value.setStudentId(studentId); value.setCourseId("CO1");
        value.setSemesterId("SEM1"); value.setDate(LocalDate.of(2026, 3, 1)); value.setStatus(status); return value;
    }
}
