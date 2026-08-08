package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.common.StudentNoResolver;
import com.campus.education.dto.leave.LeaveApprovalResult;
import com.campus.education.dto.leave.LeaveBatchApprovalResult;
import com.campus.education.dto.leave.LeaveRequestCreateRequest;
import com.campus.education.dto.leave.LeaveSyncFailure;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.Course;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.LeaveRequest;
import com.campus.education.entity.Semester;
import com.campus.education.entity.Student;
import com.campus.education.entity.User;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.CourseScheduleMapper;
import com.campus.education.mapper.LeaveRequestMapper;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.UserMapper;
import com.campus.education.service.CourseRosterService;
import com.campus.education.service.LeaveRequestService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest>
        implements LeaveRequestService {

    private static final Set<String> LEAVE_TYPES = new HashSet<>();

    static {
        Collections.addAll(LEAVE_TYPES, "sick", "personal", "official", "other");
    }

    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final SemesterMapper semesterMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final AttendanceMapper attendanceMapper;
    private final UserMapper userMapper;
    private final CourseRosterService courseRosterService;

    public LeaveRequestServiceImpl(StudentMapper studentMapper,
                                   CourseMapper courseMapper,
                                   SemesterMapper semesterMapper,
                                   CourseScheduleMapper courseScheduleMapper,
                                   AttendanceMapper attendanceMapper,
                                   UserMapper userMapper,
                                   CourseRosterService courseRosterService) {
        this.studentMapper = studentMapper;
        this.courseMapper = courseMapper;
        this.semesterMapper = semesterMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.attendanceMapper = attendanceMapper;
        this.userMapper = userMapper;
        this.courseRosterService = courseRosterService;
    }

    /** 创建时同时做课程归属、学期范围与活动申请重复校验。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveRequest create(LeaveRequestCreateRequest request, String studentId) {
        LeaveRequest entity = new LeaveRequest();
        entity.setStudentId(trim(studentId));
        // 课程为可选关联；空值必须落库为 NULL，避免触发课程外键约束。
        entity.setCourseId(trimToNull(request.getCourseId()));
        entity.setSemesterId(trim(request.getSemesterId()));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setLeaveType(trim(request.getLeaveType()).toLowerCase());
        entity.setReason(trim(request.getReason()));

        List<LeaveSyncFailure> failures = validateApplication(entity);
        if (!failures.isEmpty()) throw new BusinessException(400, failures.get(0).getReason());

        LambdaQueryWrapper<LeaveRequest> overlapQuery = new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, entity.getStudentId())
                .eq(LeaveRequest::getSemesterId, entity.getSemesterId())
                .in(LeaveRequest::getStatus, "pending", "approved")
                .le(LeaveRequest::getStartDate, entity.getEndDate())
                .ge(LeaveRequest::getEndDate, entity.getStartDate());
        if (entity.getCourseId() == null) {
            overlapQuery.isNull(LeaveRequest::getCourseId);
        } else {
            overlapQuery.eq(LeaveRequest::getCourseId, entity.getCourseId());
        }
        Long overlapCount = baseMapper.selectCount(overlapQuery);
        if (overlapCount != null && overlapCount > 0) {
            throw new BusinessException(409, "相同课程和学期已有日期重叠的待审批或已通过请假申请");
        }

        entity.setRequestHash(fingerprint(entity));
        entity.setStatus("pending");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setAttendanceSyncStatus("pending");
        try {
            save(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "该请假申请已提交，请勿重复操作");
        }
        return enrich(Collections.singletonList(entity)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveRequest cancel(String requestId, String studentId) {
        LeaveRequest request = findForUpdate(requestId);
        if (request == null) throw new BusinessException(404, "请假申请不存在");
        if (!request.getStudentId().equals(studentId)) throw new BusinessException(403, "不能撤销其他学生的请假申请");
        if (!"pending".equals(request.getStatus())) throw new BusinessException(409, "只有待审批申请可以撤销");
        request.setStatus("cancelled");
        request.setAttendanceSyncStatus("not_required");
        request.setSyncFailureReason(null);
        updateById(request);
        return enrich(Collections.singletonList(request)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveRequest reject(String requestId, String processorUserId, String processOpinion) {
        LeaveRequest request = findForUpdate(requestId);
        if (request == null) throw new BusinessException(404, "请假申请不存在");
        if (!"pending".equals(request.getStatus())) throw new BusinessException(409, "只有待审批申请可以拒绝");
        request.setStatus("rejected");
        request.setProcessedBy(processorUserId);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessOpinion(trimToNull(processOpinion));
        request.setAttendanceSyncStatus("not_required");
        request.setSyncFailureReason(null);
        updateById(request);
        return enrich(Collections.singletonList(request)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveApprovalResult approve(String requestId, String processorUserId, String processOpinion) {
        return approveLocked(requestId, processorUserId, processOpinion);
    }

    /** 批量业务失败互不覆盖；任一数据库或系统异常继续向外抛出并回滚整个事务。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveBatchApprovalResult batchApprove(List<String> requestIds, String processorUserId, String processOpinion) {
        LeaveBatchApprovalResult batch = new LeaveBatchApprovalResult();
        Set<String> seen = new HashSet<>();
        for (String requestId : requestIds) {
            LeaveApprovalResult result;
            String normalizedId = trim(requestId);
            if (normalizedId.isEmpty() || !seen.add(normalizedId)) {
                result = failed(normalizedId, null, "requestId", "申请单ID为空或在本次请求中重复", "移除重复项后重新审批。");
            } else {
                result = approveLocked(normalizedId, processorUserId, processOpinion);
            }
            batch.getResults().add(result);
            if (result.isApproved()) batch.setSuccessCount(batch.getSuccessCount() + 1);
            else batch.setFailedCount(batch.getFailedCount() + 1);
        }
        return batch;
    }

    private LeaveApprovalResult approveLocked(String requestId, String processorUserId, String processOpinion) {
        LeaveRequest request = findForUpdate(requestId);
        if (request == null) return failed(requestId, null, "requestId", "请假申请不存在", "刷新列表后重新选择申请。");
        if ("approved".equals(request.getStatus()) && "synced".equals(request.getAttendanceSyncStatus())) {
            LeaveApprovalResult result = successResult(request, countDates(request), true);
            return result;
        }
        if (!"pending".equals(request.getStatus())) {
            return failed(requestId, null, "status", "只有待审批申请可以通过", "刷新列表并核对申请当前状态。");
        }

        List<LeaveSyncFailure> failures = validateApplication(request);
        Map<LocalDate, Attendance> existingByDate = loadExistingAttendance(request);
        if (failures.isEmpty() && trim(request.getCourseId()).isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            request.setStatus("approved");
            request.setProcessedBy(processorUserId);
            request.setProcessedAt(now);
            request.setProcessOpinion(trimToNull(processOpinion));
            request.setAttendanceSyncStatus("not_required");
            request.setSyncFailureReason(null);
            updateById(request);
            LeaveApprovalResult result = successResult(request, 0, false);
            result.setAttendanceSyncStatus("not_required");
            return result;
        }
        if (failures.isEmpty()) {
            LocalDate date = request.getStartDate();
            while (!date.isAfter(request.getEndDate())) {
                Attendance existing = existingByDate.get(date);
                if (existing != null) {
                    if (Boolean.TRUE.equals(existing.getManualLocked())) {
                        failures.add(failure(requestId, date, "attendance", "该日期考勤已被人工锁定", "联系考勤管理员核对并解除锁定后重试。"));
                    } else if (!"leave".equals(existing.getStatus())) {
                        failures.add(failure(requestId, date, "attendance", "该日期已有非请假考勤：" + existing.getStatus(), "先核对已有考勤事实，系统不会静默覆盖。"));
                    } else if ("leave_request".equals(existing.getRecordSource())
                            && !requestId.equals(existing.getSourceRequestId())) {
                        failures.add(failure(requestId, date, "attendance", "该日期已关联其他请假申请", "核对重复或重叠申请后再审批。"));
                    }
                }
                date = date.plusDays(1);
            }
        }

        if (!failures.isEmpty()) {
            request.setAttendanceSyncStatus("failed");
            request.setSyncFailureReason(summarize(failures));
            updateById(request);
            LeaveApprovalResult result = new LeaveApprovalResult();
            result.setRequestId(requestId);
            result.setAttendanceSyncStatus("failed");
            result.setFailureDetails(failures);
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        int synchronizedCount = 0;
        LocalDate date = request.getStartDate();
        while (!date.isAfter(request.getEndDate())) {
            Attendance attendance = existingByDate.get(date);
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setStudentId(request.getStudentId());
                attendance.setCourseId(request.getCourseId());
                attendance.setSemesterId(request.getSemesterId());
                attendance.setDate(date);
                attendance.setStatus("leave");
                attendance.setRecordSource("leave_request");
                attendance.setSourceRequestId(requestId);
                attendance.setApprovedBy(processorUserId);
                attendance.setSyncedAt(now);
                attendance.setManualLocked(false);
                attendanceMapper.insert(attendance);
            } else if (!requestId.equals(attendance.getSourceRequestId())
                    || !"leave_request".equals(attendance.getRecordSource())) {
                attendance.setStatus("leave");
                attendance.setRecordSource("leave_request");
                attendance.setSourceRequestId(requestId);
                attendance.setApprovedBy(processorUserId);
                attendance.setSyncedAt(now);
                attendance.setManualLocked(false);
                attendanceMapper.updateById(attendance);
            }
            synchronizedCount++;
            date = date.plusDays(1);
        }

        request.setStatus("approved");
        request.setProcessedBy(processorUserId);
        request.setProcessedAt(now);
        request.setProcessOpinion(trimToNull(processOpinion));
        request.setAttendanceSyncStatus("synced");
        request.setSyncFailureReason(null);
        updateById(request);
        return successResult(request, synchronizedCount, false);
    }

    private List<LeaveSyncFailure> validateApplication(LeaveRequest request) {
        List<LeaveSyncFailure> failures = new ArrayList<>();
        Student student = studentMapper.selectById(request.getStudentId());
        if (student == null) failures.add(failure(request.getRequestId(), null, "studentId", "学生不存在", "核对学生档案后重试。"));
        else if (!"active".equals(student.getStatus())) failures.add(failure(request.getRequestId(), null, "studentId", "学生不是在籍状态", "恢复有效学籍或终止审批。"));

        Course course = trim(request.getCourseId()).isEmpty() ? null : courseMapper.selectById(request.getCourseId());
        if (!trim(request.getCourseId()).isEmpty() && course == null) {
            failures.add(failure(request.getRequestId(), null, "courseId", "课程不存在", "重新选择有效课程。"));
        }
        Semester semester = semesterMapper.selectById(request.getSemesterId());
        if (semester == null) failures.add(failure(request.getRequestId(), null, "semesterId", "学期不存在", "重新选择有效学期。"));

        if (request.getStartDate() == null || request.getEndDate() == null) {
            failures.add(failure(request.getRequestId(), null, "dateRange", "请假日期范围不完整", "填写开始和结束日期。"));
        } else if (request.getEndDate().isBefore(request.getStartDate())) {
            failures.add(failure(request.getRequestId(), request.getStartDate(), "dateRange", "结束日期不能早于开始日期", "重新选择日期范围。"));
        } else if (semester != null && ((semester.getStartDate() != null && request.getStartDate().isBefore(semester.getStartDate()))
                || (semester.getEndDate() != null && request.getEndDate().isAfter(semester.getEndDate())))) {
            failures.add(failure(request.getRequestId(), request.getStartDate(), "dateRange", "请假日期不在所选学期范围内", "将日期调整到该学期起止日期之间。"));
        }
        if (!LEAVE_TYPES.contains(request.getLeaveType())) {
            failures.add(failure(request.getRequestId(), null, "leaveType", "请假类型无效", "选择病假、事假、公假或其他。"));
        }
        if (trim(request.getReason()).isEmpty()) {
            failures.add(failure(request.getRequestId(), null, "reason", "请假原因不能为空", "填写可供审批人核对的原因。"));
        }

        if (course != null && semester != null && student != null) {
            Long scheduleCount = courseScheduleMapper.selectCount(new LambdaQueryWrapper<CourseSchedule>()
                    .eq(CourseSchedule::getCourseId, request.getCourseId())
                    .eq(CourseSchedule::getSemesterId, request.getSemesterId()));
            if (scheduleCount == null || scheduleCount == 0) {
                failures.add(failure(request.getRequestId(), null, "courseId", "该课程在所选学期没有排课", "核对课程和学期后重新提交。"));
            } else {
                boolean onRoster = courseRosterService.listActiveStudents(request.getCourseId(), request.getSemesterId(), null)
                        .stream().anyMatch(item -> request.getStudentId().equals(item.getStudentId()));
                if (!onRoster) failures.add(failure(request.getRequestId(), null, "studentId", "学生不在该课程花名册中", "先完成选课或班级排课关系维护。"));
            }
        }
        return failures;
    }

    private Map<LocalDate, Attendance> loadExistingAttendance(LeaveRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null || trim(request.getCourseId()).isEmpty()) {
            return Collections.emptyMap();
        }
        List<Attendance> rows = attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, request.getStudentId())
                .eq(Attendance::getCourseId, request.getCourseId())
                .eq(Attendance::getSemesterId, request.getSemesterId())
                .between(Attendance::getDate, request.getStartDate(), request.getEndDate())
                .last("FOR UPDATE"));
        return rows.stream().collect(Collectors.toMap(Attendance::getDate, Function.identity(), (left, right) -> left));
    }

    private LeaveRequest findForUpdate(String requestId) {
        if (trim(requestId).isEmpty()) return null;
        return baseMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getRequestId, requestId)
                .last("LIMIT 1 FOR UPDATE"));
    }

    private LeaveApprovalResult successResult(LeaveRequest request, int count, boolean idempotent) {
        LeaveApprovalResult result = new LeaveApprovalResult();
        result.setRequestId(request.getRequestId());
        result.setApproved(true);
        result.setIdempotent(idempotent);
        result.setSynchronizedCount(count);
        result.setAttendanceSyncStatus("synced");
        return result;
    }

    private LeaveApprovalResult failed(String requestId, LocalDate date, String field, String reason, String suggestion) {
        LeaveApprovalResult result = new LeaveApprovalResult();
        result.setRequestId(requestId);
        result.setAttendanceSyncStatus("failed");
        result.getFailureDetails().add(failure(requestId, date, field, reason, suggestion));
        return result;
    }

    private LeaveSyncFailure failure(String requestId, LocalDate date, String field, String reason, String suggestion) {
        return new LeaveSyncFailure(requestId, date, field, reason, suggestion);
    }

    private int countDates(LeaveRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) return 0;
        return (int) (request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1);
    }

    private String summarize(List<LeaveSyncFailure> failures) {
        String value = failures.stream().map(item -> (item.getDate() == null ? "" : item.getDate() + "：") + item.getReason())
                .collect(Collectors.joining("；"));
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private String fingerprint(LeaveRequest request) {
        String raw = String.join("|", request.getStudentId(), request.getCourseId(), request.getSemesterId(),
                String.valueOf(request.getStartDate()), String.valueOf(request.getEndDate()), request.getLeaveType(), request.getReason());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /** 列表展示信息只做关联补全，不改变持久化字段。 */
    @Override
    public List<LeaveRequest> enrich(List<LeaveRequest> requests) {
        if (requests == null || requests.isEmpty()) return requests;
        Map<String, Student> students = mapStudents(requests);
        Map<String, Course> courses = mapByIds(requests.stream().map(LeaveRequest::getCourseId).filter(this::notBlank).collect(Collectors.toSet()), courseMapper::selectBatchIds);
        Map<String, Semester> semesters = mapByIds(requests.stream().map(LeaveRequest::getSemesterId).filter(this::notBlank).collect(Collectors.toSet()), semesterMapper::selectBatchIds);
        Map<String, User> users = mapByIds(requests.stream().map(LeaveRequest::getProcessedBy).filter(this::notBlank).collect(Collectors.toSet()), userMapper::selectBatchIds);
        for (LeaveRequest request : requests) {
            Student student = students.get(request.getStudentId());
            Course course = courses.get(request.getCourseId());
            Semester semester = semesters.get(request.getSemesterId());
            User user = users.get(request.getProcessedBy());
            if (student != null) {
                request.setStudentNo(student.getStudentNo());
                request.setStudentName(student.getName());
            }
            if (course != null) request.setCourseName(course.getName());
            if (semester != null) request.setSemesterName(semester.getName());
            if (user != null) request.setProcessedByName(user.getName());
        }
        return requests;
    }

    private Map<String, Student> mapStudents(List<LeaveRequest> requests) {
        Set<String> ids = requests.stream().map(LeaveRequest::getStudentId).filter(this::notBlank).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        List<Student> students = studentMapper.selectBatchIds(ids);
        StudentNoResolver.fillDisplayStudentNos(students);
        return students.stream().collect(Collectors.toMap(Student::getStudentId, Function.identity(), (left, right) -> left));
    }

    private <T> Map<String, T> mapByIds(Set<String> ids, Function<Set<String>, List<T>> loader) {
        if (ids.isEmpty()) return Collections.emptyMap();
        Map<String, T> result = new HashMap<>();
        for (T item : loader.apply(ids)) {
            String id;
            if (item instanceof Course) id = ((Course) item).getCourseId();
            else if (item instanceof Semester) id = ((Semester) item).getSemesterId();
            else id = ((User) item).getUserId();
            result.put(id, item);
        }
        return result;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
