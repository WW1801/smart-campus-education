package com.campus.education.service.impl;

/**
 * 课程名单服务实现类，负责处理课程名单相关业务逻辑。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.Student;
import com.campus.education.entity.StudentCourseSelection;
import com.campus.education.mapper.CourseScheduleMapper;
import com.campus.education.mapper.StudentCourseSelectionMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.CourseRosterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseRosterServiceImpl implements CourseRosterService {

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentCourseSelectionMapper studentCourseSelectionMapper;

    // 查询在籍学生列表
    @Override
    public List<Student> listActiveStudents(String courseId, String semesterId, String teacherId) {
        if (isBlank(courseId) || isBlank(semesterId)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getCourseId, courseId.trim())
                .eq(CourseSchedule::getSemesterId, semesterId.trim());
        if (!isBlank(teacherId)) {
            wrapper.eq(CourseSchedule::getTeacherId, teacherId.trim());
        }
        wrapper.orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod, CourseSchedule::getScheduleId);

        List<CourseSchedule> schedules = courseScheduleMapper.selectList(wrapper);
        if (schedules.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Student> rosterMap = new LinkedHashMap<>();
        for (CourseSchedule schedule : schedules) {
            if ("class_based".equals(schedule.getMode())) {
                addStudents(rosterMap, listActiveStudentsByClass(schedule.getClassId()));
                continue;
            }
            addStudents(rosterMap, listActiveStudentsBySelection(schedule.getScheduleId()));
        }

        return rosterMap.values().stream()
                .sorted(Comparator.comparing(Student::getStudentId, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    // 按排课主键获取单个教学班名单。
    @Override
    public List<Student> listActiveStudentsBySchedule(String scheduleId) {
        if (isBlank(scheduleId)) return Collections.emptyList();
        CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId.trim());
        if (schedule == null) return Collections.emptyList();
        return "class_based".equals(schedule.getMode())
                ? listActiveStudentsByClass(schedule.getClassId())
                : listActiveStudentsBySelection(schedule.getScheduleId());
    }

    // 查询在籍学生按班级列表
    private List<Student> listActiveStudentsByClass(String classId) {
        if (isBlank(classId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getClassId, classId.trim())
                .eq(Student::getStatus, "active")
                .orderByAsc(Student::getStudentId);
        return studentMapper.selectList(wrapper);
    }

    // 查询在籍学生按选课列表
    private List<Student> listActiveStudentsBySelection(String scheduleId) {
        if (isBlank(scheduleId)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<StudentCourseSelection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourseSelection::getScheduleId, scheduleId.trim())
                .ne(StudentCourseSelection::getStatus, "dropped")
                .orderByAsc(StudentCourseSelection::getSelectionTime);
        List<StudentCourseSelection> selections = studentCourseSelectionMapper.selectList(wrapper);
        if (selections.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderedStudentIds = selections.stream()
                .map(StudentCourseSelection::getStudentId)
                .filter(id -> !isBlank(id))
                .collect(Collectors.toList());
        Set<String> studentIds = new LinkedHashSet<>(orderedStudentIds);
        if (studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .filter(student -> "active".equals(student.getStatus()))
                .collect(Collectors.toMap(Student::getStudentId, item -> item, (left, right) -> left));

        List<Student> students = new ArrayList<>();
        for (String studentId : orderedStudentIds) {
            Student student = studentMap.get(studentId);
            if (student != null) {
                students.add(student);
            }
        }
        return students;
    }

    // 添加学生
    private void addStudents(Map<String, Student> rosterMap, List<Student> students) {
        for (Student student : students) {
            rosterMap.putIfAbsent(student.getStudentId(), student);
        }
    }

    // 判断是否为空白
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
