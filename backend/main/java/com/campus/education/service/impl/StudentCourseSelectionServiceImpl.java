package com.campus.education.service.impl;

/**
 * 学生选课服务实现类，负责处理学生选课相关业务逻辑。
 */

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Autowired
    private SemesterMapper semesterMapper;

    @Autowired
    private ClassroomMapper classroomMapper;

    // 处理选课
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

    // 刷新排课数据
    private CourseSchedule freshSchedule(String scheduleId) {
        return courseScheduleMapper.selectById(scheduleId);
    }

    // 处理退选课程
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

    // 获取我的课程
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

    // 获取我的课表
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
        Set<String> classroomIds = scheduleMap.values().stream()
                .map(CourseSchedule::getClassroomId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> semesterIds = scheduleMap.values().stream()
                .map(CourseSchedule::getSemesterId).filter(Objects::nonNull).collect(Collectors.toSet());

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
        Map<String, Classroom> classroomMap = new HashMap<>();
        if (!classroomIds.isEmpty()) {
            classroomMapper.selectBatchIds(classroomIds).forEach(c ->
                    classroomMap.put(c.getClassroomId(), c));
        }
        Map<String, Semester> semesterMap = new HashMap<>();
        if (!semesterIds.isEmpty()) {
            semesterMapper.selectBatchIds(semesterIds).forEach(s ->
                    semesterMap.put(s.getSemesterId(), s));
        }

        List<Map<String, Object>> scheduleList = new ArrayList<>();
        for (StudentCourseSelection sel : selections) {
            CourseSchedule cs = scheduleMap.get(sel.getScheduleId());
            if (cs == null) continue;

            Course course = courseMap.get(cs.getCourseId());
            Teacher teacher = teacherMap.get(cs.getTeacherId());
            Classroom classroom = classroomMap.get(cs.getClassroomId());
            Semester semester = semesterMap.get(cs.getSemesterId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("selectionId", sel.getSelectionId());
            item.put("courseId", cs.getCourseId());
            item.put("courseName", course != null ? course.getName() : "");
            item.put("credits", course != null ? course.getCredits() : 0);
            item.put("teacherName", teacher != null ? teacher.getName() : "");
            item.put("classroomId", cs.getClassroomId());
            item.put("classroomName", classroom != null ? classroom.getName() : "");
            item.put("classroom", classroom != null ? classroom.getName() : cs.getClassroomId());
            item.put("weeks", formatWeeks(semester));
            item.put("dayOfWeek", cs.getDayOfWeek());
            item.put("startPeriod", cs.getStartPeriod());
            item.put("endPeriod", cs.getEndPeriod());
            item.put("semesterId", cs.getSemesterId());
            item.put("status", sel.getStatus());
            scheduleList.add(item);
        }
        return scheduleList;
    }

    // 获取学生课表
    @Override
    public List<Map<String, Object>> getStudentTimetable(String studentId, String semesterId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("瀛︾敓涓嶅瓨鍦?");
        }

        String resolvedSemesterId = resolveSemesterId(semesterId);
        if (resolvedSemesterId == null) {
            return Collections.emptyList();
        }

        Map<String, CourseSchedule> mergedSchedules = new LinkedHashMap<>();
        if (student.getClassId() != null && !student.getClassId().trim().isEmpty()) {
            LambdaQueryWrapper<CourseSchedule> classWrapper = new LambdaQueryWrapper<>();
            classWrapper.eq(CourseSchedule::getClassId, student.getClassId())
                    .eq(CourseSchedule::getSemesterId, resolvedSemesterId)
                    .orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartPeriod);
            courseScheduleMapper.selectList(classWrapper).forEach(item -> mergedSchedules.put(item.getScheduleId(), item));
        }

        List<String> selectedScheduleIds = getMyCourses(studentId, resolvedSemesterId).stream()
                .map(StudentCourseSelection::getScheduleId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (!selectedScheduleIds.isEmpty()) {
            courseScheduleMapper.selectBatchIds(selectedScheduleIds).stream()
                    .filter(item -> resolvedSemesterId.equals(item.getSemesterId()))
                    .forEach(item -> mergedSchedules.put(item.getScheduleId(), item));
        }

        if (mergedSchedules.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> courseIds = mergedSchedules.values().stream()
                .map(CourseSchedule::getCourseId)
                .collect(Collectors.toSet());
        Set<String> teacherIds = mergedSchedules.values().stream()
                .map(CourseSchedule::getTeacherId)
                .collect(Collectors.toSet());
        Set<String> classroomIds = mergedSchedules.values().stream()
                .map(CourseSchedule::getClassroomId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());
        Set<String> semesterIds = mergedSchedules.values().stream()
                .map(CourseSchedule::getSemesterId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());

        Map<String, Course> courseMap = courseIds.isEmpty()
                ? Collections.emptyMap()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, item -> item, (left, right) -> left));
        Map<String, Teacher> teacherMap = teacherIds.isEmpty()
                ? Collections.emptyMap()
                : teacherMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(Teacher::getTeacherId, item -> item, (left, right) -> left));
        Map<String, Classroom> classroomMap = classroomIds.isEmpty()
                ? Collections.emptyMap()
                : classroomMapper.selectBatchIds(classroomIds).stream()
                .collect(Collectors.toMap(Classroom::getClassroomId, item -> item, (left, right) -> left));
        Map<String, Semester> semesterMap = semesterIds.isEmpty()
                ? Collections.emptyMap()
                : semesterMapper.selectBatchIds(semesterIds).stream()
                .collect(Collectors.toMap(Semester::getSemesterId, item -> item, (left, right) -> left));

        return mergedSchedules.values().stream()
                .sorted(Comparator.comparing(CourseSchedule::getDayOfWeek)
                        .thenComparing(CourseSchedule::getStartPeriod)
                        .thenComparing(CourseSchedule::getEndPeriod))
                .map(schedule -> {
                    Course course = courseMap.get(schedule.getCourseId());
                    Teacher teacher = teacherMap.get(schedule.getTeacherId());
                    Classroom classroom = classroomMap.get(schedule.getClassroomId());
                    Semester semester = semesterMap.get(schedule.getSemesterId());

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("scheduleId", schedule.getScheduleId());
                    item.put("courseId", schedule.getCourseId());
                    item.put("courseName", course != null ? course.getName() : schedule.getCourseId());
                    item.put("credits", course != null ? course.getCredits() : 0);
                    item.put("teacherName", teacher != null ? teacher.getName() : schedule.getTeacherId());
                    item.put("semesterId", schedule.getSemesterId());
                    item.put("classroomId", schedule.getClassroomId());
                    item.put("classroomName", classroom != null ? classroom.getName() : "");
                    item.put("classroom", classroom != null ? classroom.getName() : schedule.getClassroomId());
                    item.put("weeks", formatWeeks(semester));
                    item.put("dayOfWeek", schedule.getDayOfWeek());
                    item.put("startPeriod", schedule.getStartPeriod());
                    item.put("endPeriod", schedule.getEndPeriod());
                    item.put("mode", schedule.getMode());
                    item.put("status", "open_selection".equals(schedule.getMode()) ? "selected" : "class_based");
                    return item;
                })
                .collect(Collectors.toList());
    }

    // 获取可选课程
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
            item.put("classroomId", cs.getClassroomId());
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

    private String formatWeeks(Semester semester) {
        if (semester == null || semester.getTeachingWeeks() == null || semester.getTeachingWeeks() <= 0) {
            return "";
        }
        return "1-" + semester.getTeachingWeeks() + "周";
    }

    // 获取前置课程名称
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

    // 检查前置课程是否满足
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

    // 仅判断前置课程是否满足
    private boolean checkPrerequisiteMet(String studentId, String courseId, String majorId) {
        try {
            checkPrerequisites(studentId, courseId, majorId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    // 检查时间冲突
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

    // 判断是否存在时间冲突
    private boolean checkTimeConflictExists(String studentId, CourseSchedule newSchedule) {
        try {
            checkTimeConflict(studentId, newSchedule);
            return false;
        } catch (BusinessException e) {
            return true;
        }
    }

    // 解析学期编号
    private String resolveSemesterId(String semesterId) {
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            return semesterId.trim();
        }

        LocalDate businessDate = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Semester currentSemester = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .le(Semester::getStartDate, businessDate)
                .ge(Semester::getEndDate, businessDate)
                .orderByDesc(Semester::getStartDate)
                .last("LIMIT 1"));
        if (currentSemester != null) {
            return currentSemester.getSemesterId();
        }

        Semester upcomingSemester = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .gt(Semester::getStartDate, businessDate)
                .orderByAsc(Semester::getStartDate)
                .last("LIMIT 1"));
        if (upcomingSemester != null) {
            return upcomingSemester.getSemesterId();
        }

        Semester latestSemester = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .orderByDesc(Semester::getStartDate)
                .last("LIMIT 1"));
        return latestSemester != null ? latestSemester.getSemesterId() : null;
    }
}
