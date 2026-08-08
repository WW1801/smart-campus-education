package com.campus.education.controller.attendance;

/**
 * 考勤控制器，负责处理考勤相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessException;
import com.campus.education.common.Result;
import com.campus.education.common.StudentAccessGuard;
import com.campus.education.common.StudentNoResolver;
import com.campus.education.dto.attendance.BatchAttendanceRequest;
import com.campus.education.entity.Attendance;
import com.campus.education.entity.Course;
import com.campus.education.entity.Semester;
import com.campus.education.entity.Student;
import com.campus.education.entity.User;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.AttendanceService;
import com.campus.education.service.CourseRosterService;
import com.campus.education.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private StudentAccessGuard studentAccessGuard;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private SemesterMapper semesterMapper;

    @Autowired
    private CourseRosterService courseRosterService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer limit,
                                            @RequestParam(required = false) String semesterId,
                                            @RequestParam(required = false) String courseId,
                                            @RequestParam(required = false) String studentId,
                                            @RequestParam(required = false) String studentNo,
                                            @RequestParam(required = false) String classId,
                                            @RequestParam(required = false) String date,
                                            @RequestParam(required = false) String status,
                                            Authentication authentication) {
        studentId = studentAccessGuard.resolveStudentFilter(authentication, studentId);
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Attendance::getSemesterId, semesterId);
        }
        if (courseId != null && !courseId.trim().isEmpty()) {
            wrapper.eq(Attendance::getCourseId, courseId);
        }
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.eq(Attendance::getStudentId, studentId);
        }
        if (studentNo != null && !studentNo.trim().isEmpty()) {
            List<Student> candidates = studentMapper.selectList(null);
            StudentNoResolver.fillDisplayStudentNos(candidates);
            List<String> matchedStudentIds = candidates.stream()
                    .filter(student -> student.getStudentNo() != null
                            && student.getStudentNo().contains(studentNo.trim()))
                    .map(Student::getStudentId).collect(Collectors.toList());
            if (matchedStudentIds.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("records", java.util.Collections.emptyList());
                empty.put("total", 0);
                empty.put("current", page.longValue());
                empty.put("size", limit.longValue());
                return Result.success("查询成功", empty);
            }
            wrapper.in(Attendance::getStudentId, matchedStudentIds);
        }
        if (classId != null && !classId.trim().isEmpty()) {
            List<String> studentIds = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .eq(Student::getClassId, classId.trim()))
                    .stream()
                    .map(Student::getStudentId)
                    .collect(Collectors.toList());
            if (studentIds.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("records", java.util.Collections.emptyList());
                empty.put("total", 0);
                empty.put("current", page.longValue());
                empty.put("size", limit.longValue());
                return Result.success("鏌ヨ鎴愬姛", empty);
            }
            wrapper.in(Attendance::getStudentId, studentIds);
        }
        LocalDate queryDate = parseDate(date);
        if (queryDate != null) {
            wrapper.eq(Attendance::getDate, queryDate);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Attendance::getStatus, status);
        }
        wrapper.orderByDesc(Attendance::getDate);
        Page<Attendance> pageParam = new Page<>(page, limit);
        IPage<Attendance> result = attendanceService.page(pageParam, wrapper);
        enrichAttendances(result.getRecords());

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success("鏌ヨ鎴愬姛", data);
    }

    // 处理名单
    @GetMapping("/roster")
    public Result<List<Attendance>> roster(@RequestParam String courseId,
                                           @RequestParam(required = false) String semesterId,
                                           @RequestParam(required = false) String date,
                                           @RequestParam(required = false) String teacherId,
                                           Authentication authentication) {
        if (courseId == null || courseId.trim().isEmpty()) {
            return Result.badRequest("璇疯緭鍏ヨ绋婭D");
        }

        String resolvedSemesterId = resolveSemesterId(semesterId, date);
        if (resolvedSemesterId == null) {
            return Result.badRequest("鏈壘鍒板搴斿鏈?");
        }

        String resolvedTeacherId = resolveTeacherId(authentication, teacherId);
        List<Student> rosterStudents = resolveDisplayStudentNos(
                courseRosterService.listActiveStudents(courseId, resolvedSemesterId, resolvedTeacherId));

        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendance::getCourseId, courseId)
                .eq(Attendance::getSemesterId, resolvedSemesterId)
                .orderByDesc(Attendance::getUpdatedAt, Attendance::getCreatedAt);
        LocalDate attendanceDate = parseDate(date);
        if (attendanceDate != null) {
            wrapper.eq(Attendance::getDate, attendanceDate);
        }
        List<Attendance> attendanceRecords = attendanceService.list(wrapper);
        enrichAttendances(attendanceRecords);

        Map<String, Attendance> attendanceMap = attendanceRecords.stream()
                .filter(item -> item.getStudentId() != null && !item.getStudentId().trim().isEmpty())
                .collect(Collectors.toMap(Attendance::getStudentId, Function.identity(), this::pickLatestAttendance, LinkedHashMap::new));

        Course course = courseMapper.selectById(courseId);
        List<Attendance> result = new ArrayList<>();
        Set<String> includedStudentIds = new LinkedHashSet<>();

        for (Student student : rosterStudents) {
            Attendance attendance = attendanceMap.get(student.getStudentId());
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setStudentId(student.getStudentId());
                attendance.setCourseId(courseId);
                attendance.setSemesterId(resolvedSemesterId);
                attendance.setDate(attendanceDate);
            }
            attendance.setStudentName(student.getName());
            attendance.setStudentNo(student.getStudentNo());
            attendance.setCourseName(course != null ? course.getName() : courseId);
            result.add(attendance);
            includedStudentIds.add(student.getStudentId());
        }

        for (Attendance attendance : attendanceMap.values()) {
            if (includedStudentIds.add(attendance.getStudentId())) {
                if (attendance.getCourseName() == null && course != null) {
                    attendance.setCourseName(course.getName());
                }
                result.add(attendance);
            }
        }

        result.sort(Comparator.comparing(Attendance::getStudentId, Comparator.nullsLast(String::compareTo)));
        return Result.success(result);
    }

    // 添加考勤
    @PostMapping
    public Result<Void> add(@RequestBody Attendance attendance) {
        normalizeAttendancePayload(attendance, false);
        attendanceService.saveManual(attendance);
        return Result.success("保存成功", null);
    }

    // 更新考勤
    @PutMapping
    public Result<Void> update(@RequestBody Attendance attendance) {
        normalizeAttendancePayload(attendance, true);
        attendanceService.updateManual(attendance);
        return Result.success("更新成功", null);
    }

    // 删除考勤
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        attendanceService.removeManual(id);
        return Result.success("删除成功", null);
    }

    // 批量保存
    @PostMapping("/batch-save")
    public Result<Map<String, Object>> batchSave(@RequestBody BatchAttendanceRequest request,
                                                  Authentication authentication) {
        String teacherId = resolveTeacherId(authentication, null);
        return Result.success("批量考勤处理完成", attendanceService.batchSave(request, teacherId));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(required = false) String semesterId,
                                                  @RequestParam(required = false) String courseId,
                                                  @RequestParam(required = false) String classId) {
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Attendance::getSemesterId, semesterId);
        }
        if (courseId != null && !courseId.trim().isEmpty()) {
            wrapper.eq(Attendance::getCourseId, courseId);
        }

        long totalCount = attendanceService.count(wrapper);
        long presentCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "present"));
        long lateCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "late"));
        long absentCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "absent"));
        long leaveCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "leave"));
        long earlyCount = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                .eq(Attendance::getStatus, "early"));

        Map<String, Object> data = new HashMap<>();
        data.put("totalClasses", totalCount);
        data.put("presentCount", presentCount);
        data.put("lateCount", lateCount);
        data.put("earlyCount", earlyCount);
        data.put("absentCount", absentCount);
        data.put("leaveCount", leaveCount);
        data.put("attendanceRate", totalCount > 0 ? String.format("%.1f%%", (double) presentCount / totalCount * 100) : "0%");

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String[] dayNames = {"鍛ㄤ竴", "鍛ㄤ簩", "鍛ㄤ笁", "鍛ㄥ洓", "鍛ㄤ簲", "鍛ㄥ叚", "鍛ㄦ棩"};
        for (int i = 6; i >= 0; i--) {
            LocalDate queryDate = today.minusDays(i);
            long dayTotal = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                    .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                    .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                    .eq(Attendance::getDate, queryDate));
            long dayPresent = attendanceService.count(new LambdaQueryWrapper<Attendance>()
                    .eq(semesterId != null && !semesterId.trim().isEmpty(), Attendance::getSemesterId, semesterId)
                    .eq(courseId != null && !courseId.trim().isEmpty(), Attendance::getCourseId, courseId)
                    .eq(Attendance::getDate, queryDate)
                    .eq(Attendance::getStatus, "present"));
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", dayNames[queryDate.getDayOfWeek().getValue() - 1]);
            dayData.put("rate", dayTotal > 0 ? Math.round((double) dayPresent / dayTotal * 100) : 0);
            trend.add(dayData);
        }
        data.put("trend", trend);

        return Result.success("鏌ヨ鎴愬姛", data);
    }

    // 补全考勤信息
    private void enrichAttendances(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            return;
        }

        Set<String> studentIds = attendances.stream()
                .map(Attendance::getStudentId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());
        Set<String> courseIds = attendances.stream()
                .map(Attendance::getCourseId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());

        Map<String, Student> studentMap = studentIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : resolveDisplayStudentNos(studentMapper.selectBatchIds(studentIds)).stream()
                .collect(Collectors.toMap(Student::getStudentId, Function.identity(), (left, right) -> left));
        Map<String, Course> courseMap = courseIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(), (left, right) -> left));

        for (Attendance attendance : attendances) {
            Student student = studentMap.get(attendance.getStudentId());
            if (student != null) {
                attendance.setStudentName(student.getName());
                attendance.setStudentNo(student.getStudentNo());
            }
            Course course = courseMap.get(attendance.getCourseId());
            if (course != null) {
                attendance.setCourseName(course.getName());
            }
        }
    }

    private List<Student> resolveDisplayStudentNos(List<Student> students) {
        if (students == null || students.isEmpty()) return java.util.Collections.emptyList();
        if (students.stream().allMatch(StudentNoResolver::isStandard)) return students;
        List<Student> allStudents = studentMapper.selectList(null);
        StudentNoResolver.fillDisplayStudentNos(allStudents);
        Map<String, Student> resolved = allStudents.stream()
                .collect(Collectors.toMap(Student::getStudentId, Function.identity(), (left, right) -> left));
        return students.stream().map(student -> resolved.getOrDefault(student.getStudentId(), student))
                .collect(Collectors.toList());
    }

    // 选取最新考勤记录
    private Attendance pickLatestAttendance(Attendance left, Attendance right) {
        LocalDateTime leftTime = left.getUpdatedAt() != null ? left.getUpdatedAt() : left.getCreatedAt();
        LocalDateTime rightTime = right.getUpdatedAt() != null ? right.getUpdatedAt() : right.getCreatedAt();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return rightTime.isAfter(leftTime) ? right : left;
    }

    // 解析教师编号
    private String resolveTeacherId(Authentication authentication, String requestedTeacherId) {
        if (authentication == null || authentication.getName() == null) {
            return requestedTeacherId;
        }
        User user = userService.getById(authentication.getName());
        if (user == null) {
            return requestedTeacherId;
        }
        if ("4".equals(user.getRoleId())) {
            return user.getRelatedId();
        }
        return requestedTeacherId;
    }

    // 解析学期编号
    private String resolveSemesterId(String requestedSemesterId, String date) {
        if (requestedSemesterId != null && !requestedSemesterId.trim().isEmpty()) {
            return requestedSemesterId.trim();
        }

        LocalDate queryDate = parseDate(date);
        if (queryDate == null) {
            queryDate = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        }
        Semester matchedSemester = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .le(Semester::getStartDate, queryDate)
                .ge(Semester::getEndDate, queryDate)
                .orderByDesc(Semester::getStartDate)
                .last("LIMIT 1"));
        return matchedSemester != null ? matchedSemester.getSemesterId() : null;
    }

    // 解析日期
    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(date.trim());
    }

    // 规范化考勤请求数据
    private void normalizeAttendancePayload(Attendance attendance, boolean requireId) {
        if (attendance == null) {
            throw new BusinessException(400, "璇锋彁渚涙湁鏁堢殑鑰冨嫟鏁版嵁");
        }
        if (requireId && isBlank(attendance.getAttendanceId())) {
            throw new BusinessException(400, "缂轰慨鑰冨嫟璁板綍ID");
        }

        attendance.setAttendanceId(trimToNull(attendance.getAttendanceId()));
        attendance.setStudentId(trimToNull(attendance.getStudentId()));
        attendance.setCourseId(trimToNull(attendance.getCourseId()));
        attendance.setSemesterId(trimToNull(attendance.getSemesterId()));
        attendance.setStatus(normalizeAttendanceStatus(attendance.getStatus()));

        if (isBlank(attendance.getStudentId())) {
            throw new BusinessException(400, "璇疯緭鍏ュ鍙?");
        }
        if (studentMapper.selectById(attendance.getStudentId()) == null) {
            throw new BusinessException(400, "瀛︾敓涓嶅瓨鍦紝璇烽€夋嫨鏈夋晥瀛﹀彿");
        }

        if (isBlank(attendance.getCourseId())) {
            throw new BusinessException(400, "璇疯緭鍏ヨ绋婭D");
        }
        if (courseMapper.selectById(attendance.getCourseId()) == null) {
            throw new BusinessException(400, "璇剧▼涓嶅瓨鍦紝璇烽€夋嫨鏈夋晥璇剧▼");
        }

        if (attendance.getDate() == null) {
            throw new BusinessException(400, "璇烽€夋嫨鑰冨嫟鏃ユ湡");
        }

        attendance.setSemesterId(resolveValidSemesterId(attendance.getSemesterId(), attendance.getDate()));
    }

    // 解析有效学期编号
    private String resolveValidSemesterId(String requestedSemesterId, LocalDate attendanceDate) {
        if (!isBlank(requestedSemesterId) && semesterMapper.selectById(requestedSemesterId) != null) {
            return requestedSemesterId;
        }

        String resolvedSemesterId = resolveSemesterId(null, attendanceDate != null ? attendanceDate.toString() : null);
        if (!isBlank(resolvedSemesterId)) {
            return resolvedSemesterId;
        }

        throw new BusinessException(400, "璇烽€夋嫨鏈夋晥瀛︽湡");
    }

    // 规范化考勤状态
    private String normalizeAttendanceStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            throw new BusinessException(400, "璇烽€夋嫨鑰冨嫟鐘舵€?");
        }
        if ("early_leave".equalsIgnoreCase(normalized)) {
            normalized = "early";
        }

        switch (normalized) {
            case "present":
            case "late":
            case "early":
            case "absent":
            case "leave":
                return normalized;
            default:
                throw new BusinessException(400, "璇烽€夋嫨鏈夋晥鐨勮€冨嫟鐘舵€?");
        }
    }

    // 去除首尾空格并转换空值
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // 判断是否为空白
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
