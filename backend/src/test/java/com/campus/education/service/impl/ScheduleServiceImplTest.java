package com.campus.education.service.impl;

import com.campus.education.dto.schedule.AutoArrangeRequest;
import com.campus.education.entity.Classroom;
import com.campus.education.entity.Course;
import com.campus.education.entity.Schedule;
import com.campus.education.entity.Semester;
import com.campus.education.entity.Teacher;
import com.campus.education.mapper.ClassMapper;
import com.campus.education.mapper.ClassroomMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.ScheduleMapper;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeacherMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleServiceImplTest {
    private ScheduleServiceImpl service;
    private ScheduleMapper scheduleMapper;
    private CourseMapper courseMapper;
    private TeacherMapper teacherMapper;
    private ClassMapper classMapper;
    private ClassroomMapper classroomMapper;
    private SemesterMapper semesterMapper;
    private StudentMapper studentMapper;
    private final AtomicInteger ids = new AtomicInteger();

    @BeforeEach
    void setUp() {
        service = new ScheduleServiceImpl();
        scheduleMapper = mock(ScheduleMapper.class);
        courseMapper = mock(CourseMapper.class);
        teacherMapper = mock(TeacherMapper.class);
        classMapper = mock(ClassMapper.class);
        classroomMapper = mock(ClassroomMapper.class);
        semesterMapper = mock(SemesterMapper.class);
        studentMapper = mock(StudentMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", scheduleMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "teacherMapper", teacherMapper);
        ReflectionTestUtils.setField(service, "classMapper", classMapper);
        ReflectionTestUtils.setField(service, "classroomMapper", classroomMapper);
        ReflectionTestUtils.setField(service, "semesterMapper", semesterMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);

        when(semesterMapper.selectById("SEM1")).thenReturn(new Semester());
        when(semesterMapper.lockById("SEM1")).thenReturn("SEM1");
        when(courseMapper.selectById(any())).thenAnswer(invocation -> { Object id = invocation.getArgument(0); return course(String.valueOf(id)); });
        when(teacherMapper.selectById(any())).thenAnswer(invocation -> { Object id = invocation.getArgument(0); return teacher(String.valueOf(id)); });
        when(classMapper.selectById(any())).thenReturn(new com.campus.education.entity.Class());
        when(studentMapper.selectCount(any())).thenReturn(20L);
        when(scheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.insert(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule value = invocation.getArgument(0);
            value.setScheduleId("SCH" + ids.incrementAndGet());
            return 1;
        });
    }

    @Test
    void shouldReturnRealTeacherConflictAndSuggestion() {
        assertRealConflict("teacher", occupied("OLD", "OLD-CO", "T1", "C9", "R9"), "T1", "C1", "R1", "请调整时间段或更换教师。");
    }

    @Test
    void shouldReturnRealClassConflict() {
        assertRealConflict("class", occupied("OLD", "OLD-CO", "T9", "C1", "R9"), "T1", "C1", "R1", "请调整时间段或更换班级。");
    }

    @Test
    void shouldReturnRealClassroomConflict() {
        assertRealConflict("classroom", occupied("OLD", "OLD-CO", "T9", "C9", "R1"), "T1", "C1", "R1", "请选择其他候选教室或调整时间段。");
    }

    @Test
    void shouldReportCapacityShortage() {
        Classroom room = classroom("R1", "小教室", 10);
        when(classroomMapper.selectById("R1")).thenReturn(room);
        Map<String, Object> result = service.autoArrange(request(Collections.singletonList(task("CO1", "T1", "C1")),
                Collections.singletonList(slot(1, 1, 2)), Collections.singletonList("R1")));
        Map<?, ?> failure = firstFailure(result);
        assertEquals("capacity", failure.get("type"));
        assertEquals("P1", failure.get("priority"));
        assertNull(failure.get("conflictClassroomName"));
        assertEquals("小教室", failure.get("candidateClassroomName"));
        assertEquals(10, failure.get("candidateClassroomCapacity"));
        assertEquals(20, failure.get("requiredCapacity"));
        assertEquals("请选择容量足够的教室。", failure.get("suggestion"));
        assertNull(failure.get("conflictScheduleId"));
    }

    @Test
    void shouldReportNoAvailableTimeSlot() {
        when(classroomMapper.selectById("R1")).thenReturn(classroom("R1", "第一教室", 50));
        Map<String, Object> result = service.autoArrange(request(Collections.singletonList(task("CO1", "T1", "C1")),
                Collections.singletonList(slot(1, 4, 2)), Collections.singletonList("R1")));
        Map<?, ?> failure = firstFailure(result);
        assertEquals("time", failure.get("type"));
        assertEquals("P3", failure.get("priority"));
        assertEquals("请增加候选时间段后重新提交。", failure.get("suggestion"));
    }

    @Test
    void shouldArrangeMultipleTasksInOneBatch() {
        when(classroomMapper.selectById("R1")).thenReturn(classroom("R1", "第一教室", 50));
        List<AutoArrangeRequest.Task> tasks = Arrays.asList(task("CO1", "T1", "C1"), task("CO2", "T2", "C2"));
        Map<String, Object> result = service.autoArrange(request(tasks,
                Arrays.asList(slot(1, 1, 2), slot(2, 1, 2)), Collections.singletonList("R1")));
        assertEquals(2, result.get("arrangedCount"));
        assertEquals(0, result.get("failedCount"));
    }

    @Test
    void shouldReturnPartialSuccess() {
        when(classroomMapper.selectById("R1")).thenReturn(classroom("R1", "第一教室", 50));
        when(courseMapper.selectById("BAD")).thenReturn(null);
        Map<String, Object> result = service.autoArrange(request(
                Arrays.asList(task("CO1", "T1", "C1"), task("BAD", "T2", "C2")),
                Collections.singletonList(slot(1, 1, 2)), Collections.singletonList("R1")));
        assertEquals(1, result.get("arrangedCount"));
        assertEquals(1, result.get("failedCount"));
    }

    @Test
    void shouldReturnAllFailed() {
        when(classroomMapper.selectById("R1")).thenReturn(classroom("R1", "第一教室", 50));
        when(scheduleMapper.selectList(any())).thenReturn(Collections.singletonList(occupied("OLD", "OLD-CO", "T1", "C9", "R9")));
        Map<String, Object> result = service.autoArrange(request(
                Arrays.asList(task("CO1", "T1", "C1"), task("CO2", "T1", "C2")),
                Collections.singletonList(slot(1, 1, 2)), Collections.singletonList("R1")));
        assertEquals(0, result.get("arrangedCount"));
        assertEquals(2, result.get("failedCount"));
    }

    private void assertRealConflict(String type, Schedule occupied, String teacherId, String classId,
                                    String roomId, String suggestion) {
        when(scheduleMapper.selectList(any())).thenReturn(Collections.singletonList(occupied));
        when(classroomMapper.selectById(roomId)).thenReturn(classroom(roomId, "候选教室", 50));
        when(classroomMapper.selectById(occupied.getClassroomId())).thenReturn(classroom(occupied.getClassroomId(), "真实冲突教室", 50));
        Map<String, Object> result = service.autoArrange(request(Collections.singletonList(task("CO1", teacherId, classId)),
                Collections.singletonList(slot(1, 1, 2)), Collections.singletonList(roomId)));
        Map<?, ?> failure = firstFailure(result);
        assertEquals(type, failure.get("type"));
        assertEquals("OLD", failure.get("conflictScheduleId"));
        assertEquals("OLD-CO", failure.get("conflictCourseId"));
        assertEquals("课程OLD-CO", failure.get("conflictCourseName"));
        assertEquals("T9".equals(occupied.getTeacherId()) ? "教师T9" : "教师T1", failure.get("conflictTeacherName"));
        assertEquals("真实冲突教室", failure.get("conflictClassroomName"));
        assertEquals(1, failure.get("conflictDayOfWeek"));
        assertEquals(suggestion, failure.get("suggestion"));
    }

    private Map<?, ?> firstFailure(Map<String, Object> result) {
        assertEquals(1, result.get("failedCount"));
        return (Map<?, ?>) ((List<?>) result.get("failureDetails")).get(0);
    }

    private AutoArrangeRequest request(List<AutoArrangeRequest.Task> tasks, List<AutoArrangeRequest.TimeSlot> slots, List<String> rooms) {
        AutoArrangeRequest request = new AutoArrangeRequest();
        request.setSemesterId("SEM1"); request.setTasks(tasks); request.setTimeSlots(slots); request.setClassroomIds(rooms);
        return request;
    }

    private AutoArrangeRequest.Task task(String courseId, String teacherId, String classId) {
        AutoArrangeRequest.Task task = new AutoArrangeRequest.Task();
        task.setCourseId(courseId); task.setTeacherId(teacherId); task.setClassId(classId); task.setMode("class_based");
        return task;
    }

    private AutoArrangeRequest.TimeSlot slot(int day, int start, int end) {
        AutoArrangeRequest.TimeSlot slot = new AutoArrangeRequest.TimeSlot();
        slot.setDayOfWeek(day); slot.setStartPeriod(start); slot.setEndPeriod(end); return slot;
    }

    private Schedule occupied(String id, String courseId, String teacherId, String classId, String roomId) {
        Schedule value = new Schedule();
        value.setScheduleId(id); value.setCourseId(courseId); value.setTeacherId(teacherId); value.setClassId(classId);
        value.setClassroomId(roomId); value.setSemesterId("SEM1"); value.setDayOfWeek(1); value.setStartPeriod(1); value.setEndPeriod(2);
        return value;
    }

    private Classroom classroom(String id, String name, int capacity) {
        Classroom value = new Classroom(); value.setClassroomId(id); value.setName(name); value.setCapacity(capacity); value.setStatus("available"); return value;
    }

    private Course course(String id) { Course value = new Course(); value.setCourseId(id); value.setName("课程" + id); return value; }
    private Teacher teacher(String id) { Teacher value = new Teacher(); value.setTeacherId(id); value.setName("教师" + id); return value; }
}
