package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.entity.*;
import com.campus.education.mapper.*;
import com.campus.education.service.StudentCourseSelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentCourseSelectionServiceImpl extends ServiceImpl<StudentCourseSelectionMapper, StudentCourseSelection>
        implements StudentCourseSelectionService {

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeachingPlanMapper teachingPlanMapper;

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Override
    @Transactional
    public StudentCourseSelection selectCourse(String studentId, String scheduleId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }

        if (!"active".equals(student.getStatus())) {
            throw new BusinessException("当前学籍状态不可选课，仅在读学生可选课");
        }

        CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排课记录不存在");
        }

        if (!"open_selection".equals(schedule.getMode())) {
            throw new BusinessException("该排课为按班级排课模式，不可通过选课加入");
        }

        StudentCourseSelection existingSelection = this.getOne(new LambdaQueryWrapper<StudentCourseSelection>()
                .eq(StudentCourseSelection::getStudentId, studentId)
                .eq(StudentCourseSelection::getScheduleId, scheduleId)
                .orderByDesc(StudentCourseSelection::getCreatedAt)
                .last("LIMIT 1"));

        if (existingSelection != null) {
            if ("selected".equals(existingSelection.getStatus())) {
                throw new BusinessException("已选该课程，不可重复选课");
            }
            if ("dropped".equals(existingSelection.getStatus())) {
                existingSelection.setStatus("selected");
                existingSelection.setSelectionTime(LocalDateTime.now());
                existingSelection.setDropTime(null);
                this.updateById(existingSelection);

                courseScheduleMapper.update(null, new LambdaUpdateWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getScheduleId, scheduleId)
                        .lt(CourseSchedule::getCurrentStudents, freshSchedule(scheduleId).getMaxStudents())
                        .setSql("current_students = current_students + 1"));

                return existingSelection;
            }
        }

        checkPrerequisites(studentId, schedule.getCourseId(), student.getMajorId());

        checkTimeConflict(studentId, schedule);

        CourseSchedule freshSchedule = freshSchedule(scheduleId);
        if (freshSchedule.getMaxStudents() != null && freshSchedule.getCurrentStudents() >= freshSchedule.getMaxStudents()) {
            throw new BusinessException("选课人数已满，无法选课");
        }
        int updated = courseScheduleMapper.update(null, new LambdaUpdateWrapper<CourseSchedule>()
                .eq(CourseSchedule::getScheduleId, scheduleId)
                .lt(CourseSchedule::getCurrentStudents, freshSchedule.getMaxStudents())
                .setSql("current_students = current_students + 1"));
        if (updated == 0) {
            throw new BusinessException("选课人数已满，无法选课");
        }

        StudentCourseSelection selection = new StudentCourseSelection();
        selection.setStudentId(studentId);
        selection.setCourseId(schedule.getCourseId());
        selection.setScheduleId(scheduleId);
        selection.setSemesterId(schedule.getSemesterId());
        selection.setStatus("selected");
        selection.setSelectionTime(LocalDateTime.now());
        this.save(selection);

        return selection;
    }

    private CourseSchedule freshSchedule(String scheduleId) {
        return courseScheduleMapper.selectById(scheduleId);
    }

    @Override
    @Transactional
    public void dropCourse(String selectionId) {
        StudentCourseSelection selection = this.getById(selectionId);
        if (selection == null) {
            throw new BusinessException("选课记录不存在");
        }
        if (!"selected".equals(selection.getStatus())) {
            throw new BusinessException("仅已选状态可退选");
        }

        selection.setStatus("dropped");
        selection.setDropTime(LocalDateTime.now());
        this.updateById(selection);

        courseScheduleMapper.update(null, new LambdaUpdateWrapper<CourseSchedule>()
                .eq(CourseSchedule::getScheduleId, selection.getScheduleId())
                .gt(CourseSchedule::getCurrentStudents, 0)
                .setSql("current_students = current_students - 1"));
    }

    @Override
    public List<StudentCourseSelection> getMyCourses(String studentId, String semesterId) {
        LambdaQueryWrapper<StudentCourseSelection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourseSelection::getStudentId, studentId);
        wrapper.ne(StudentCourseSelection::getStatus, "dropped");
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(StudentCourseSelection::getSemesterId, semesterId);
        }
        wrapper.orderByDesc(StudentCourseSelection::getSelectionTime);
        return this.list(wrapper);
    }

    @Override
    public List<Map<String, Object>> getMySchedule(String studentId, String semesterId) {
        List<StudentCourseSelection> selections = getMyCourses(studentId, semesterId);
        List<String> scheduleIds = selections.stream()
                .map(StudentCourseSelection::getScheduleId)
                .collect(Collectors.toList());

        Map<String, CourseSchedule> scheduleMap = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            courseScheduleMapper.selectBatchIds(scheduleIds).forEach(s ->
                    scheduleMap.put(s.getScheduleId(), s));
        }

        Set<String> courseIds = scheduleMap.values().stream()
                .map(CourseSchedule::getCourseId).collect(Collectors.toSet());
        Set<String> teacherIds = scheduleMap.values().stream()
                .map(CourseSchedule::getTeacherId).collect(Collectors.toSet());

        Map<String, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseMapper.selectBatchIds(courseIds).forEach(c ->
                    courseMap.put(c.getCourseId(), c));
        }

        Map<String, Teacher> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            teacherMapper.selectBatchIds(teacherIds).forEach(t ->
                    teacherMap.put(t.getTeacherId(), t));
        }

        List<Map<String, Object>> scheduleList = new ArrayList<>();
        for (StudentCourseSelection sel : selections) {
            CourseSchedule cs = scheduleMap.get(sel.getScheduleId());
            if (cs == null) continue;

            Course course = courseMap.get(cs.getCourseId());
            Teacher teacher = teacherMap.get(cs.getTeacherId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("selectionId", sel.getSelectionId());
            item.put("courseId", cs.getCourseId());
            item.put("courseName", course != null ? course.getName() : "");
            item.put("credits", course != null ? course.getCredits() : 0);
            item.put("teacherName", teacher != null ? teacher.getName() : "");
            item.put("dayOfWeek", cs.getDayOfWeek());
            item.put("startPeriod", cs.getStartPeriod());
            item.put("endPeriod", cs.getEndPeriod());
            item.put("semesterId", cs.getSemesterId());
            item.put("status", sel.getStatus());
            scheduleList.add(item);
        }
        return scheduleList;
    }

    @Override
    public List<Map<String, Object>> getAvailableCourses(String studentId, String semesterId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }

        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getMode, "open_selection");
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(CourseSchedule::getSemesterId, semesterId);
        }
        List<CourseSchedule> schedules = courseScheduleMapper.selectList(wrapper);

        List<StudentCourseSelection> mySelections = this.list(new LambdaQueryWrapper<StudentCourseSelection>()
                .eq(StudentCourseSelection::getStudentId, studentId)
                .ne(StudentCourseSelection::getStatus, "dropped"));
        Set<String> selectedScheduleIds = mySelections.stream()
                .map(StudentCourseSelection::getScheduleId)
                .collect(Collectors.toSet());

        Set<String> courseIds = schedules.stream()
                .map(CourseSchedule::getCourseId).collect(Collectors.toSet());
        Set<String> teacherIds = schedules.stream()
                .map(CourseSchedule::getTeacherId).collect(Collectors.toSet());

        Map<String, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseMapper.selectBatchIds(courseIds).forEach(c ->
                    courseMap.put(c.getCourseId(), c));
        }

        Map<String, Teacher> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            teacherMapper.selectBatchIds(teacherIds).forEach(t ->
                    teacherMap.put(t.getTeacherId(), t));
        }

        List<Map<String, Object>> availableList = new ArrayList<>();
        for (CourseSchedule cs : schedules) {
            Course course = courseMap.get(cs.getCourseId());
            Teacher teacher = teacherMap.get(cs.getTeacherId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("scheduleId", cs.getScheduleId());
            item.put("courseId", cs.getCourseId());
            item.put("courseName", course != null ? course.getName() : "");
            item.put("courseCode", course != null ? course.getCode() : "");
            item.put("credits", course != null ? course.getCredits() : 0);
            item.put("teacherName", teacher != null ? teacher.getName() : "");
            item.put("dayOfWeek", cs.getDayOfWeek());
            item.put("startPeriod", cs.getStartPeriod());
            item.put("endPeriod", cs.getEndPeriod());
            item.put("maxStudents", cs.getMaxStudents());
            item.put("currentStudents", cs.getCurrentStudents());
            item.put("remaining", cs.getMaxStudents() != null ? cs.getMaxStudents() - cs.getCurrentStudents() : -1);
            item.put("alreadySelected", selectedScheduleIds.contains(cs.getScheduleId()));

            boolean prereqMet = checkPrerequisiteMet(studentId, cs.getCourseId(), student.getMajorId());
            item.put("prerequisitesMet", prereqMet);

            boolean timeConflict = checkTimeConflictExists(studentId, cs);
            item.put("hasTimeConflict", timeConflict);

            List<String> prereqNames = getPrerequisiteNames(cs.getCourseId(), student.getMajorId());
            item.put("prerequisites", prereqNames);

            availableList.add(item);
        }
        return availableList;
    }

    private List<String> getPrerequisiteNames(String courseId, String majorId) {
        LambdaQueryWrapper<TeachingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingPlan::getCourseId, courseId);
        wrapper.eq(TeachingPlan::getMajorId, majorId);
        TeachingPlan plan = teachingPlanMapper.selectOne(wrapper);

        if (plan == null || plan.getIsPrerequisite() == null || plan.getIsPrerequisite() != 1) {
            return Collections.emptyList();
        }
        if (plan.getPrerequisiteIds() == null || plan.getPrerequisiteIds().trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        String[] prereqIds = plan.getPrerequisiteIds().split(",");
        for (String prereqId : prereqIds) {
            prereqId = prereqId.trim();
            if (prereqId.isEmpty()) continue;
            Course prereqCourse = courseMapper.selectById(prereqId);
            names.add(prereqCourse != null ? prereqCourse.getName() : prereqId);
        }
        return names;
    }

    private void checkPrerequisites(String studentId, String courseId, String majorId) {
        LambdaQueryWrapper<TeachingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingPlan::getCourseId, courseId);
        wrapper.eq(TeachingPlan::getMajorId, majorId);
        TeachingPlan plan = teachingPlanMapper.selectOne(wrapper);

        if (plan == null || plan.getIsPrerequisite() == null || plan.getIsPrerequisite() != 1) {
            return;
        }
        if (plan.getPrerequisiteIds() == null || plan.getPrerequisiteIds().trim().isEmpty()) {
            return;
        }

        String[] prereqIds = plan.getPrerequisiteIds().split(",");
        for (String prereqId : prereqIds) {
            prereqId = prereqId.trim();
            if (prereqId.isEmpty()) continue;

            Long passCount = gradeMapper.selectCount(new LambdaQueryWrapper<Grade>()
                    .eq(Grade::getStudentId, studentId)
                    .eq(Grade::getCourseId, prereqId)
                    .eq(Grade::getStatus, "approved")
                    .eq(Grade::getIsPass, true));
            if (passCount == 0) {
                Course prereqCourse = courseMapper.selectById(prereqId);
                String prereqName = prereqCourse != null ? prereqCourse.getName() : prereqId;
                throw new BusinessException("先修课程未通过：" + prereqName + "，不可选课");
            }
        }
    }

    private boolean checkPrerequisiteMet(String studentId, String courseId, String majorId) {
        try {
            checkPrerequisites(studentId, courseId, majorId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private void checkTimeConflict(String studentId, CourseSchedule newSchedule) {
        List<StudentCourseSelection> mySelections = this.list(new LambdaQueryWrapper<StudentCourseSelection>()
                .eq(StudentCourseSelection::getStudentId, studentId)
                .ne(StudentCourseSelection::getStatus, "dropped"));

        List<String> scheduleIds = mySelections.stream()
                .map(StudentCourseSelection::getScheduleId)
                .collect(Collectors.toList());

        Map<String, CourseSchedule> existingMap = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            courseScheduleMapper.selectBatchIds(scheduleIds).forEach(s ->
                    existingMap.put(s.getScheduleId(), s));
        }

        for (StudentCourseSelection sel : mySelections) {
            CourseSchedule existing = existingMap.get(sel.getScheduleId());
            if (existing == null) continue;

            if (existing.getSemesterId().equals(newSchedule.getSemesterId())
                    && existing.getDayOfWeek().equals(newSchedule.getDayOfWeek())) {
                if (existing.getStartPeriod() <= newSchedule.getEndPeriod()
                        && newSchedule.getStartPeriod() <= existing.getEndPeriod()) {
                    Course course = courseMapper.selectById(existing.getCourseId());
                    String courseName = course != null ? course.getName() : existing.getCourseId();
                    throw new BusinessException("时间冲突：与已选课程「" + courseName + "」冲突（周"
                            + existing.getDayOfWeek() + " 第" + existing.getStartPeriod() + "-" + existing.getEndPeriod() + "节）");
                }
            }
        }
    }

    private boolean checkTimeConflictExists(String studentId, CourseSchedule newSchedule) {
        try {
            checkTimeConflict(studentId, newSchedule);
            return false;
        } catch (BusinessException e) {
            return true;
        }
    }
}
