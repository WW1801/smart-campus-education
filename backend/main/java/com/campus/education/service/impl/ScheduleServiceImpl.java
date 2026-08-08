package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.dto.schedule.AutoArrangeRequest;
import com.campus.education.entity.Classroom;
import com.campus.education.entity.Course;
import com.campus.education.entity.Schedule;
import com.campus.education.entity.Teacher;
import com.campus.education.mapper.ClassMapper;
import com.campus.education.mapper.ClassroomMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.ScheduleMapper;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Autowired private CourseMapper courseMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private ClassMapper classMapper;
    @Autowired private ClassroomMapper classroomMapper;
    @Autowired private SemesterMapper semesterMapper;
    @Autowired private StudentMapper studentMapper;

    /**
     * 按请求顺序排课；每条成功记录立即加入本批次占用集合，后续任务可检测到批内冲突。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoArrange(AutoArrangeRequest request) {
        validateRequest(request);
        String semesterId = request.getSemesterId().trim();
        semesterMapper.lockById(semesterId);
        List<Schedule> occupied = new ArrayList<>(list(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getSemesterId, semesterId)));
        List<Map<String, Object>> successes = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();

        for (int index = 0; index < request.getTasks().size(); index++) {
            AutoArrangeRequest.Task task = request.getTasks().get(index);
            Failure validationFailure = validateTask(index, task);
            if (validationFailure != null) {
                failures.add(validationFailure.toMap());
                continue;
            }

            int requiredCapacity = requiredCapacity(task);
            Schedule arranged = null;
            List<Failure> candidates = new ArrayList<>();
            for (AutoArrangeRequest.TimeSlot slot : request.getTimeSlots()) {
                String slotError = validateSlot(slot);
                if (slotError != null) {
                    candidates.add(timeFailure(index, task, slotError));
                    continue;
                }
                for (String classroomIdValue : request.getClassroomIds()) {
                    String classroomId = trim(classroomIdValue);
                    Classroom classroom = classroomMapper.selectById(classroomId);
                    if (classroom == null || !isAvailable(classroom.getStatus())) {
                        candidates.add(classroomUnavailableFailure(index, task, classroom,
                                classroomId, "候选教室不存在或当前不可用"));
                        continue;
                    }
                    if (classroom.getCapacity() == null || requiredCapacity > classroom.getCapacity()) {
                        candidates.add(capacityFailure(index, task, classroom, requiredCapacity));
                        continue;
                    }

                    List<Failure> conflicts = findConflicts(index, occupied, task, classroom, slot);
                    if (!conflicts.isEmpty()) {
                        candidates.addAll(conflicts);
                        continue;
                    }

                    arranged = buildSchedule(semesterId, task, classroomId, slot);
                    save(arranged);
                    occupied.add(arranged);
                    successes.add(success(index, arranged));
                    break;
                }
                if (arranged != null) break;
            }
            if (arranged == null) {
                Failure best = candidates.stream().min(Comparator.comparingInt(Failure::rank)).orElse(null);
                if (best == null) best = timeFailure(index, task, "没有可用时间段");
                failures.add(best.toMap());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("arrangedCount", successes.size());
        result.put("failedCount", failures.size());
        result.put("successDetails", successes);
        result.put("failureDetails", failures);
        return result;
    }

    private void validateRequest(AutoArrangeRequest request) {
        if (request == null || isBlank(request.getSemesterId())) throw new BusinessException(400, "请选择学期");
        if (semesterMapper.selectById(request.getSemesterId().trim()) == null) throw new BusinessException(400, "所选学期不存在");
        if (request.getTasks() == null || request.getTasks().isEmpty()) throw new BusinessException(400, "请至少添加一个排课任务");
        if (request.getClassroomIds() == null || request.getClassroomIds().stream().allMatch(this::isBlank)) {
            throw new BusinessException(400, "请至少选择一个候选教室");
        }
        if (request.getTimeSlots() == null || request.getTimeSlots().isEmpty()) throw new BusinessException(400, "请至少添加一个候选时间段");
    }

    private Failure validateTask(int index, AutoArrangeRequest.Task task) {
        if (task == null) return timeFailure(index, null, "排课任务不能为空");
        if (isBlank(task.getCourseId()) || courseMapper.selectById(trim(task.getCourseId())) == null) return timeFailure(index, task, "所选课程不存在");
        Teacher teacher = isBlank(task.getTeacherId()) ? null : teacherMapper.selectById(trim(task.getTeacherId()));
        if (teacher == null) return timeFailure(index, task, "所选教师不存在");
        if (teacher.getStatus() != null && !"active".equals(teacher.getStatus())) return timeFailure(index, task, "所选教师当前不可授课");
        String mode = mode(task);
        if (!"class_based".equals(mode) && !"open_selection".equals(mode)) return timeFailure(index, task, "排课模式仅支持按班级或开放选课");
        if ("class_based".equals(mode) && (isBlank(task.getClassId()) || classMapper.selectById(trim(task.getClassId())) == null)) {
            return timeFailure(index, task, "所选班级不存在");
        }
        if ("open_selection".equals(mode) && (task.getMaxStudents() == null || task.getMaxStudents() <= 0)) {
            return capacityFailure(index, task, null, task.getMaxStudents() == null ? 0 : task.getMaxStudents());
        }
        return null;
    }

    private String validateSlot(AutoArrangeRequest.TimeSlot slot) {
        if (slot == null || slot.getDayOfWeek() == null || slot.getStartPeriod() == null || slot.getEndPeriod() == null) return "候选时间段字段不完整";
        if (slot.getDayOfWeek() < 1 || slot.getDayOfWeek() > 7) return "星期必须在周一至周日之间";
        if (slot.getStartPeriod() < 1 || slot.getEndPeriod() > 20 || slot.getStartPeriod() > slot.getEndPeriod()) return "开始节次不能晚于结束节次，且节次必须在1至20之间";
        return null;
    }

    private List<Failure> findConflicts(int index, List<Schedule> occupied, AutoArrangeRequest.Task task,
                                        Classroom classroom, AutoArrangeRequest.TimeSlot slot) {
        List<Failure> failures = new ArrayList<>();
        for (Schedule existing : occupied) {
            if (!slot.getDayOfWeek().equals(existing.getDayOfWeek())
                    || existing.getStartPeriod() > slot.getEndPeriod()
                    || existing.getEndPeriod() < slot.getStartPeriod()) continue;
            if (trim(task.getTeacherId()).equals(existing.getTeacherId())) failures.add(conflictFailure(index, task, "teacher", existing, classroom));
            if (!isBlank(task.getClassId()) && trim(task.getClassId()).equals(existing.getClassId())) failures.add(conflictFailure(index, task, "class", existing, classroom));
            if (classroom.getClassroomId().equals(existing.getClassroomId())) failures.add(conflictFailure(index, task, "classroom", existing, classroom));
        }
        return failures;
    }

    private int requiredCapacity(AutoArrangeRequest.Task task) {
        if ("open_selection".equals(mode(task))) return task.getMaxStudents();
        return Math.toIntExact(studentMapper.selectCount(new LambdaQueryWrapper<com.campus.education.entity.Student>()
                .eq(com.campus.education.entity.Student::getClassId, trim(task.getClassId()))
                .eq(com.campus.education.entity.Student::getStatus, "active")));
    }

    private Schedule buildSchedule(String semesterId, AutoArrangeRequest.Task task, String classroomId,
                                   AutoArrangeRequest.TimeSlot slot) {
        Schedule schedule = new Schedule();
        schedule.setMode(mode(task));
        schedule.setMaxStudents("open_selection".equals(mode(task)) ? task.getMaxStudents() : null);
        schedule.setCurrentStudents(0);
        schedule.setCourseId(trim(task.getCourseId()));
        schedule.setTeacherId(trim(task.getTeacherId()));
        schedule.setClassId(isBlank(task.getClassId()) ? null : trim(task.getClassId()));
        schedule.setSemesterId(semesterId);
        schedule.setClassroomId(classroomId);
        schedule.setDayOfWeek(slot.getDayOfWeek());
        schedule.setStartPeriod(slot.getStartPeriod());
        schedule.setEndPeriod(slot.getEndPeriod());
        return schedule;
    }

    private Map<String, Object> success(int index, Schedule schedule) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("index", index);
        item.put("scheduleId", schedule.getScheduleId());
        item.put("courseId", schedule.getCourseId());
        item.put("teacherId", schedule.getTeacherId());
        item.put("classId", schedule.getClassId());
        item.put("classroomId", schedule.getClassroomId());
        item.put("dayOfWeek", schedule.getDayOfWeek());
        item.put("startPeriod", schedule.getStartPeriod());
        item.put("endPeriod", schedule.getEndPeriod());
        return item;
    }

    private String mode(AutoArrangeRequest.Task task) { return isBlank(task.getMode()) ? "class_based" : trim(task.getMode()); }
    private boolean isAvailable(String status) { return isBlank(status) || "available".equals(status); }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private Failure timeFailure(int index, AutoArrangeRequest.Task task, String reason) {
        return new Failure(index, task, "time", "P3", reason, "请增加候选时间段后重新提交。", null, null, null, null);
    }

    private Failure classroomUnavailableFailure(int index, AutoArrangeRequest.Task task, Classroom classroom,
                                                String classroomId, String reason) {
        if (classroom == null && classroomId != null) {
            classroom = new Classroom();
            classroom.setClassroomId(classroomId);
        }
        return new Failure(index, task, "classroom", "P2", reason,
                "请选择其他候选教室或调整时间段。", null, null, classroom, null);
    }

    private Failure capacityFailure(int index, AutoArrangeRequest.Task task, Classroom classroom, int required) {
        String reason = classroom == null ? "开放选课容量必须大于0" :
                "教室容量不足：需要" + required + "人，当前教室容量为" + classroom.getCapacity() + "人";
        return new Failure(index, task, "capacity", "P1", reason, "请选择容量足够的教室。", null, null,
                classroom, required);
    }

    private Failure conflictFailure(int index, AutoArrangeRequest.Task task, String type,
                                    Schedule existing, Classroom classroom) {
        if ("teacher".equals(type)) {
            return new Failure(index, task, type, "P0", "教师在该时间段已有排课", "请调整时间段或更换教师。", existing, classroom, null, null);
        }
        if ("class".equals(type)) {
            return new Failure(index, task, type, "P0", "班级在该时间段已有排课", "请调整时间段或更换班级。", existing, classroom, null, null);
        }
        return new Failure(index, task, type, "P2", "教室在该时间段已有排课", "请选择其他候选教室或调整时间段。", existing, classroom, null, null);
    }

    private final class Failure {
        private final int index;
        private final AutoArrangeRequest.Task task;
        private final String type;
        private final String priority;
        private final String reason;
        private final String suggestion;
        private final Schedule conflict;
        private final Classroom classroom;
        private final Classroom candidateClassroom;
        private final Integer requiredCapacity;

        private Failure(int index, AutoArrangeRequest.Task task, String type, String priority, String reason,
                        String suggestion, Schedule conflict, Classroom classroom, Classroom candidateClassroom,
                        Integer requiredCapacity) {
            this.index = index; this.task = task; this.type = type; this.priority = priority;
            this.reason = reason; this.suggestion = suggestion; this.conflict = conflict; this.classroom = classroom;
            this.candidateClassroom = candidateClassroom; this.requiredCapacity = requiredCapacity;
        }

        private int rank() { return "P0".equals(priority) ? 0 : "P1".equals(priority) ? 1 : "P2".equals(priority) ? 2 : 3; }

        private Map<String, Object> toMap() {
            Course course = conflict == null ? null : courseMapper.selectById(conflict.getCourseId());
            Teacher teacher = conflict == null ? null : teacherMapper.selectById(conflict.getTeacherId());
            Classroom conflictRoom = conflict == null ? classroom : classroomMapper.selectById(conflict.getClassroomId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", index);
            item.put("taskCourseId", task == null ? null : task.getCourseId());
            item.put("type", type);
            item.put("priority", priority);
            item.put("reason", reason);
            item.put("suggestion", suggestion);
            item.put("conflictScheduleId", conflict == null ? null : conflict.getScheduleId());
            item.put("conflictCourseId", conflict == null ? null : conflict.getCourseId());
            item.put("conflictCourseName", course == null ? null : course.getName());
            item.put("conflictTeacherId", conflict == null ? null : conflict.getTeacherId());
            item.put("conflictTeacherName", teacher == null ? null : teacher.getName());
            item.put("conflictClassroomId", conflict == null ? (conflictRoom == null ? null : conflictRoom.getClassroomId()) : conflict.getClassroomId());
            item.put("conflictClassroomName", conflictRoom == null ? null : conflictRoom.getName());
            item.put("conflictDayOfWeek", conflict == null ? null : conflict.getDayOfWeek());
            item.put("conflictStartPeriod", conflict == null ? null : conflict.getStartPeriod());
            item.put("conflictEndPeriod", conflict == null ? null : conflict.getEndPeriod());
            item.put("candidateClassroomId", candidateClassroom == null ? null : candidateClassroom.getClassroomId());
            item.put("candidateClassroomName", candidateClassroom == null ? null : candidateClassroom.getName());
            item.put("candidateClassroomCapacity", candidateClassroom == null ? null : candidateClassroom.getCapacity());
            item.put("requiredCapacity", requiredCapacity);
            return item;
        }

    }
}
