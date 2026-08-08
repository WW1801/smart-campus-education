package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.campus.education.dto.leave.LeaveApprovalResult;
import com.campus.education.dto.leave.LeaveRequestCreateRequest;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.Course;
import com.campus.education.entity.LeaveRequest;
import com.campus.education.entity.Semester;
import com.campus.education.entity.Student;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.CourseScheduleMapper;
import com.campus.education.mapper.LeaveRequestMapper;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.UserMapper;
import com.campus.education.service.CourseRosterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaveRequestServiceImplTest {
    private LeaveRequestServiceImpl service;
    private LeaveRequestMapper leaveRequestMapper;
    private AttendanceMapper attendanceMapper;
    private StudentMapper studentMapper;
    private CourseMapper courseMapper;
    private SemesterMapper semesterMapper;
    private CourseScheduleMapper courseScheduleMapper;
    private CourseRosterService courseRosterService;

    @BeforeEach
    void setUp() {
        leaveRequestMapper = mock(LeaveRequestMapper.class);
        attendanceMapper = mock(AttendanceMapper.class);
        studentMapper = mock(StudentMapper.class);
        courseMapper = mock(CourseMapper.class);
        semesterMapper = mock(SemesterMapper.class);
        courseScheduleMapper = mock(CourseScheduleMapper.class);
        courseRosterService = mock(CourseRosterService.class);
        service = new LeaveRequestServiceImpl(studentMapper, courseMapper, semesterMapper,
                courseScheduleMapper, attendanceMapper, mock(UserMapper.class), courseRosterService);
        ReflectionTestUtils.setField(service, "baseMapper", leaveRequestMapper);

        when(studentMapper.selectById("S1")).thenReturn(student());
        when(courseMapper.selectById("C1")).thenReturn(new Course());
        when(semesterMapper.selectById("SEM1")).thenReturn(semester());
        when(courseScheduleMapper.selectCount(any())).thenReturn(1L);
        when(courseRosterService.listActiveStudents("C1", "SEM1", null)).thenReturn(Collections.singletonList(student()));
        when(attendanceMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void approvalWritesOneLeaveAttendancePerDateAndMarksRequestSynced() {
        LeaveRequest request = pending();
        when(leaveRequestMapper.selectOne(any())).thenReturn(request);
        AtomicInteger inserts = new AtomicInteger();
        when(attendanceMapper.insert(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance row = invocation.getArgument(0);
            assertEquals("leave", row.getStatus());
            assertEquals("leave_request", row.getRecordSource());
            assertEquals("LR1", row.getSourceRequestId());
            assertEquals("ADMIN1", row.getApprovedBy());
            inserts.incrementAndGet();
            return 1;
        });

        LeaveApprovalResult result = service.approve("LR1", "ADMIN1", "同意");

        assertTrue(result.isApproved());
        assertEquals(2, result.getSynchronizedCount());
        assertEquals(2, inserts.get());
        assertEquals("approved", request.getStatus());
        assertEquals("synced", request.getAttendanceSyncStatus());
        verify(leaveRequestMapper).updateById(request);
    }

    @Test
    void conflictingNonLeaveAttendanceReturnsDateDetailWithoutWriting() {
        LeaveRequest request = pending();
        when(leaveRequestMapper.selectOne(any())).thenReturn(request);
        Attendance existing = new Attendance();
        existing.setDate(LocalDate.of(2026, 3, 10));
        existing.setStatus("absent");
        existing.setRecordSource("manual");
        when(attendanceMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        LeaveApprovalResult result = service.approve("LR1", "ADMIN1", null);

        assertFalse(result.isApproved());
        assertEquals("failed", result.getAttendanceSyncStatus());
        assertEquals(LocalDate.of(2026, 3, 10), result.getFailureDetails().get(0).getDate());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("非请假考勤"));
        verify(attendanceMapper, never()).insert(any(Attendance.class));
        assertEquals("pending", request.getStatus());
        assertEquals("failed", request.getAttendanceSyncStatus());
    }

    @Test
    void repeatedApprovalIsIdempotent() {
        LeaveRequest request = pending();
        request.setStatus("approved");
        request.setAttendanceSyncStatus("synced");
        when(leaveRequestMapper.selectOne(any())).thenReturn(request);

        LeaveApprovalResult result = service.approve("LR1", "ADMIN1", null);

        assertTrue(result.isApproved());
        assertTrue(result.isIdempotent());
        verify(attendanceMapper, never()).insert(any(Attendance.class));
    }

    @Test
    void cancelledOrRejectedRequestCannotBeApproved() {
        LeaveRequest request = pending();
        request.setStatus("cancelled");
        request.setAttendanceSyncStatus("not_required");
        when(leaveRequestMapper.selectOne(any())).thenReturn(request);

        LeaveApprovalResult result = service.approve("LR1", "ADMIN1", null);

        assertFalse(result.isApproved());
        assertEquals("status", result.getFailureDetails().get(0).getField());
    }

    @Test
    void studentCannotCancelAnotherStudentsRequest() {
        when(leaveRequestMapper.selectOne(any())).thenReturn(pending());
        assertThrows(RuntimeException.class, () -> service.cancel("LR1", "S2"));
    }

    @Test
    void createAllowsBlankCourseAndPersistsNullAssociation() {
        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest();
        request.setStudentId("S1");
        request.setCourseId(" ");
        request.setSemesterId("SEM1");
        request.setStartDate(LocalDate.of(2026, 3, 10));
        request.setEndDate(LocalDate.of(2026, 3, 11));
        request.setLeaveType("sick");
        request.setReason("发热就医");
        when(leaveRequestMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(leaveRequestMapper.insert(any(LeaveRequest.class))).thenAnswer(invocation -> 1);

        LeaveRequest created = service.create(request, "S1");

        assertEquals(null, created.getCourseId());
        verify(leaveRequestMapper).insert(any(LeaveRequest.class));
    }

    private LeaveRequest pending() {
        LeaveRequest request = new LeaveRequest();
        request.setRequestId("LR1");
        request.setStudentId("S1");
        request.setCourseId("C1");
        request.setSemesterId("SEM1");
        request.setStartDate(LocalDate.of(2026, 3, 10));
        request.setEndDate(LocalDate.of(2026, 3, 11));
        request.setLeaveType("sick");
        request.setReason("发热就医");
        request.setStatus("pending");
        request.setAttendanceSyncStatus("pending");
        return request;
    }

    private Student student() {
        Student student = new Student();
        student.setStudentId("S1");
        student.setStatus("active");
        return student;
    }

    private Semester semester() {
        Semester semester = new Semester();
        semester.setStartDate(LocalDate.of(2026, 2, 1));
        semester.setEndDate(LocalDate.of(2026, 7, 31));
        return semester;
    }
}
