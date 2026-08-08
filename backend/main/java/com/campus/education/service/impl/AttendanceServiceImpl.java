package com.campus.education.service.impl;

/**
 * 考勤服务实现类，负责处理考勤相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.common.BusinessException;
import com.campus.education.common.StudentNoResolver;
import com.campus.education.dto.attendance.BatchAttendanceRequest;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.Semester;
import com.campus.education.entity.Student;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.CourseScheduleMapper;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.AttendanceService;
import com.campus.education.service.CourseRosterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance> implements AttendanceService {

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    @Autowired
    private SemesterMapper semesterMapper;
    @Autowired
    private CourseRosterService courseRosterService;

    @Override
    public java.util.List<Attendance> listByStudentForWarning(String studentId, String semesterId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 固定查询条件限定学生和学期，避免 Agent 通过参数扩展查询范围。
        return this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, studentId.trim())
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId));
    }

    /** 人工新增或按唯一业务键更新考勤，禁止覆盖请假同步记录和人工锁定记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveManual(Attendance attendance) {
        Attendance existing = baseMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, attendance.getStudentId())
                .eq(Attendance::getCourseId, attendance.getCourseId())
                .eq(Attendance::getSemesterId, attendance.getSemesterId())
                .eq(Attendance::getDate, attendance.getDate())
                .last("LIMIT 1 FOR UPDATE"));
        if (existing == null) {
            applyManualMetadata(attendance);
            save(attendance);
            return;
        }
        assertManualMutable(existing);
        attendance.setAttendanceId(existing.getAttendanceId());
        applyManualMetadata(attendance);
        updateById(attendance);
    }

    /** 人工修改只能作用于未锁定且非请假同步产生的记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateManual(Attendance attendance) {
        Attendance existing = baseMapper.selectById(attendance.getAttendanceId());
        if (existing == null) throw new BusinessException(404, "考勤记录不存在");
        assertManualMutable(existing);
        applyManualMetadata(attendance);
        updateById(attendance);
    }

    /** 删除同样遵守来源和人工锁定保护，避免破坏已审批请假的考勤事实。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeManual(String attendanceId) {
        Attendance existing = baseMapper.selectById(attendanceId);
        if (existing == null) throw new BusinessException(404, "考勤记录不存在");
        assertManualMutable(existing);
        removeById(attendanceId);
    }

    /** 业务校验按行汇总；任一非预期持久化异常向外抛出，由事务整体回滚。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchSave(BatchAttendanceRequest request, String teacherId) {
        if (request == null || request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new BusinessException(400, "请至少提交一条考勤记录");
        }

        List<Map<String, Object>> successDetails = new ArrayList<>();
        List<Map<String, Object>> failureDetails = new ArrayList<>();
        Set<String> requestKeys = new HashSet<>();
        Map<String, Semester> semesterCache = new HashMap<>();
        Map<String, Set<String>> rosterCache = new HashMap<>();
        List<Student> studentNoScope = studentMapper.selectList(null);
        StudentNoResolver.fillDisplayStudentNos(studentNoScope);
        Map<String, Student> resolvedStudents = studentNoScope.stream()
                .collect(Collectors.toMap(Student::getStudentId, item -> item, (left, right) -> left));

        for (int index = 0; index < request.getRecords().size(); index++) {
            BatchAttendanceRequest.Record row = request.getRecords().get(index);
            NormalizedRow normalized = normalize(request, row);
            ValidationError error = validate(normalized, teacherId, semesterCache, rosterCache, resolvedStudents);
            String key = normalized.studentId + "|" + normalized.courseId + "|" + normalized.semesterId + "|" + normalized.date;
            if (error == null && !requestKeys.add(key)) {
                error = new ValidationError("records", "请求内存在相同学生、课程、学期和日期的重复记录", "请保留该学生的一条记录后重新保存。");
            }
            if (error != null) {
                failureDetails.add(failureDetail(index, normalized, error));
                continue;
            }

            Attendance existing = baseMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getStudentId, normalized.studentId)
                    .eq(Attendance::getCourseId, normalized.courseId)
                    .eq(Attendance::getSemesterId, normalized.semesterId)
                    .eq(Attendance::getDate, normalized.date)
                    .last("LIMIT 1"));
            ValidationError writeConflict = manualWriteConflict(existing);
            if (writeConflict != null) {
                failureDetails.add(failureDetail(index, normalized, writeConflict));
                continue;
            }
            String operation;
            if (existing == null) {
                Attendance attendance = toEntity(normalized);
                save(attendance);
                existing = attendance;
                operation = "inserted";
            } else {
                existing.setSemesterId(normalized.semesterId);
                existing.setStatus(normalized.status);
                applyManualMetadata(existing);
                updateById(existing);
                operation = "updated";
            }
            successDetails.add(successDetail(index, normalized, existing.getAttendanceId(), operation));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successDetails.size());
        result.put("failedCount", failureDetails.size());
        result.put("successDetails", successDetails);
        result.put("failureDetails", failureDetails);
        return result;
    }

    private NormalizedRow normalize(BatchAttendanceRequest request, BatchAttendanceRequest.Record row) {
        NormalizedRow result = new NormalizedRow();
        if (row == null) return result;
        result.studentId = trim(row.getStudentId());
        result.courseId = trim(first(row.getCourseId(), request.getCourseId()));
        result.semesterId = trim(first(row.getSemesterId(), request.getSemesterId()));
        result.date = row.getDate() == null ? request.getDate() : row.getDate();
        result.status = trim(row.getStatus()).toLowerCase();
        if ("early_leave".equals(result.status)) result.status = "early";
        return result;
    }

    private ValidationError validate(NormalizedRow row, String teacherId, Map<String, Semester> semesterCache,
                                     Map<String, Set<String>> rosterCache, Map<String, Student> resolvedStudents) {
        if (isBlank(row.studentId)) return error("studentId", "学号不能为空", "请填写学生学号。");
        Student student = studentMapper.selectById(row.studentId);
        row.studentName = student == null ? null : student.getName();
        Student displayStudent = student == null ? null : resolvedStudents.getOrDefault(row.studentId, student);
        row.studentNo = displayStudent == null ? null : displayStudent.getStudentNo();
        if (student == null) return error("studentId", "学生不存在", "请核对学号，或先在学生管理中维护该学生。");
        if (!"active".equals(student.getStatus())) return error("studentId", "学生不是在籍状态", "请选择在籍学生，或先更新学生学籍状态。");
        if (isBlank(row.courseId) || courseMapper.selectById(row.courseId) == null) return error("courseId", "课程不存在", "请重新选择有效课程。");
        if (isBlank(row.semesterId)) return error("semesterId", "学期不能为空", "请选择考勤所属学期。");
        Semester semester = semesterCache.get(row.semesterId);
        if (!semesterCache.containsKey(row.semesterId)) {
            semester = semesterMapper.selectById(row.semesterId);
            semesterCache.put(row.semesterId, semester);
        }
        if (semester == null) return error("semesterId", "学期不存在", "请重新选择有效学期。");
        if (row.date == null) return error("date", "考勤日期不能为空", "请选择考勤日期。");
        if ((semester.getStartDate() != null && row.date.isBefore(semester.getStartDate()))
                || (semester.getEndDate() != null && row.date.isAfter(semester.getEndDate()))) {
            return error("date", "考勤日期不在学期范围内", "请选择该学期起止日期范围内的日期。");
        }
        if (!validStatus(row.status)) return error("status", "考勤状态无效", "请选择出勤、缺勤、迟到、请假或早退。");
        if (!isBlank(teacherId)) {
            Long permissionCount = courseScheduleMapper.selectCount(new LambdaQueryWrapper<CourseSchedule>()
                    .eq(CourseSchedule::getCourseId, row.courseId)
                    .eq(CourseSchedule::getSemesterId, row.semesterId)
                    .eq(CourseSchedule::getTeacherId, teacherId.trim()));
            if (permissionCount == null || permissionCount == 0) {
                return error("teacherId", "当前教师没有该课程在本学期的授课权限", "请切换到本人授课课程，或联系教务管理员维护排课。");
            }
            String rosterKey = row.courseId + "|" + row.semesterId + "|" + teacherId.trim();
            Set<String> roster = rosterCache.get(rosterKey);
            if (roster == null) {
                roster = courseRosterService.listActiveStudents(row.courseId, row.semesterId, teacherId.trim()).stream()
                        .map(Student::getStudentId).collect(Collectors.toSet());
                rosterCache.put(rosterKey, roster);
            }
            if (!roster.contains(row.studentId)) return error("studentId", "学生不在该课程花名册中", "请核对学生和课程，或先维护选课/班级排课关系。");
        }
        return null;
    }

    private Attendance toEntity(NormalizedRow row) {
        Attendance entity = new Attendance();
        entity.setStudentId(row.studentId);
        entity.setCourseId(row.courseId);
        entity.setSemesterId(row.semesterId);
        entity.setDate(row.date);
        entity.setStatus(row.status);
        applyManualMetadata(entity);
        return entity;
    }

    private void applyManualMetadata(Attendance attendance) {
        attendance.setRecordSource("manual");
        attendance.setSourceRequestId(null);
        attendance.setApprovedBy(null);
        attendance.setSyncedAt(null);
        if (attendance.getManualLocked() == null) attendance.setManualLocked(false);
    }

    private void assertManualMutable(Attendance existing) {
        ValidationError error = manualWriteConflict(existing);
        if (error != null) throw new BusinessException(409, error.reason + "；" + error.suggestion);
    }

    private ValidationError manualWriteConflict(Attendance existing) {
        if (existing == null) return null;
        if (Boolean.TRUE.equals(existing.getManualLocked())) {
            return error("attendanceId", "该考勤记录已被人工锁定", "请先由有权限的管理员解除锁定后再修改。");
        }
        if ("leave_request".equals(existing.getRecordSource())) {
            return error("attendanceId", "该考勤由已审批请假申请同步生成，不能人工覆盖", "请核对关联请假申请，保留审批后的考勤结果。");
        }
        return null;
    }

    private Map<String, Object> successDetail(int index, NormalizedRow row, String attendanceId, String operation) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("index", index);
        item.put("attendanceId", attendanceId);
        item.put("studentId", row.studentId);
        item.put("studentNo", row.studentNo);
        item.put("studentName", row.studentName);
        item.put("courseId", row.courseId);
        item.put("semesterId", row.semesterId);
        item.put("date", row.date);
        item.put("operation", operation);
        return item;
    }

    private Map<String, Object> failureDetail(int index, NormalizedRow row, ValidationError error) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("index", index);
        item.put("studentId", row.studentId);
        item.put("studentNo", row.studentNo);
        item.put("studentName", row.studentName);
        item.put("reason", error.reason);
        item.put("field", error.field);
        item.put("suggestion", error.suggestion);
        return item;
    }

    private ValidationError error(String field, String reason, String suggestion) {
        return new ValidationError(field, reason, suggestion);
    }

    private boolean validStatus(String status) {
        return "present".equals(status) || "absent".equals(status) || "late".equals(status)
                || "leave".equals(status) || "early".equals(status);
    }

    private String first(String value, String fallback) { return isBlank(value) ? fallback : value; }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private static class NormalizedRow {
        private String studentId = "";
        private String courseId = "";
        private String semesterId = "";
        private LocalDate date;
        private String status = "";
        private String studentName;
        private String studentNo;
    }

    private static class ValidationError {
        private final String field;
        private final String reason;
        private final String suggestion;

        private ValidationError(String field, String reason, String suggestion) {
            this.field = field;
            this.reason = reason;
            this.suggestion = suggestion;
        }
    }
}
