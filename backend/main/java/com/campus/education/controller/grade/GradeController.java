package com.campus.education.controller.grade;

/**
 * 成绩控制器，负责处理成绩相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.common.BusinessException;
import com.campus.education.common.StudentAccessGuard;
import com.campus.education.entity.Course;
import com.campus.education.entity.CourseSchedule;
import com.campus.education.entity.Grade;
import com.campus.education.entity.Student;
import com.campus.education.entity.User;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.CourseRosterService;
import com.campus.education.service.CourseScheduleService;
import com.campus.education.service.GradeService;
import com.campus.education.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentAccessGuard studentAccessGuard;

    @Autowired
    private CourseRosterService courseRosterService;

    @Autowired
    private CourseScheduleService courseScheduleService;

    @Autowired
    private UserService userService;

    // 分页查询成绩
    @GetMapping("/page")
    public Result<IPage<Grade>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        studentId = studentAccessGuard.resolveStudentFilter(authentication, studentId);
        Page<Grade> page = new Page<>(current, size);
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Grade::getSemesterId, semesterId);
        }
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.eq(Grade::getStudentId, studentId);
        }
        if (courseId != null && !courseId.trim().isEmpty()) {
            wrapper.eq(Grade::getCourseId, courseId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Grade::getStatus, status);
        }
        wrapper.orderByDesc(Grade::getCreatedAt);
        IPage<Grade> result = gradeService.page(page, wrapper);
        enrichGrades(result.getRecords());
        return Result.success(result);
    }

    // 处理名单
    @GetMapping("/roster")
    public Result<List<Grade>> roster(@RequestParam String courseId,
                                      @RequestParam String semesterId,
                                      @RequestParam String scheduleId,
                                      @RequestParam(required = false) String classId,
                                      @RequestParam(required = false) String teacherId,
                                      Authentication authentication) {
        if (courseId == null || courseId.trim().isEmpty() || semesterId == null || semesterId.trim().isEmpty()
                || scheduleId == null || scheduleId.trim().isEmpty()) {
            return Result.badRequest("璇疯緭鍏ヨ绋婭D鍜屽鏈烮D");
        }

        CourseSchedule schedule = courseScheduleService.getById(scheduleId);
        if (schedule == null || !courseId.equals(schedule.getCourseId()) || !semesterId.equals(schedule.getSemesterId())
                || (classId != null && !classId.trim().isEmpty() && !classId.equals(schedule.getClassId()))) {
            return Result.badRequest("排课上下文与课程、学期或班级不一致");
        }
        String resolvedTeacherId = resolveTeacherId(authentication, teacherId);
        String scheduleTeacherId = schedule.getTeacherId();
        if (resolvedTeacherId != null && !resolvedTeacherId.trim().isEmpty()
                && !resolvedTeacherId.equals(scheduleTeacherId)) {
            return Result.forbidden("当前教师没有该排课的成绩录入权限");
        }
        List<Student> rosterStudents = courseRosterService.listActiveStudentsBySchedule(schedule.getScheduleId());

        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Grade::getCourseId, courseId)
                .eq(Grade::getSemesterId, semesterId)
                .orderByDesc(Grade::getUpdatedAt, Grade::getCreatedAt);
        if (scheduleTeacherId != null && !scheduleTeacherId.trim().isEmpty()) {
            wrapper.eq(Grade::getTeacherId, scheduleTeacherId);
        }
        List<Grade> gradeRecords = gradeService.list(wrapper);
        enrichGrades(gradeRecords);

        Map<String, Grade> gradeMap = gradeRecords.stream()
                .filter(item -> item.getStudentId() != null && !item.getStudentId().trim().isEmpty())
                .collect(Collectors.toMap(Grade::getStudentId, Function.identity(), this::pickLatestGrade, LinkedHashMap::new));

        Course course = courseMapper.selectById(courseId);
        List<Grade> result = new ArrayList<>();
        Set<String> includedStudentIds = new LinkedHashSet<>();

        for (Student student : rosterStudents) {
            Grade grade = gradeMap.get(student.getStudentId());
            if (grade == null) {
                grade = new Grade();
                grade.setStudentId(student.getStudentId());
                grade.setCourseId(courseId);
                grade.setSemesterId(semesterId);
                grade.setTeacherId(scheduleTeacherId);
                grade.setStatus("draft");
            }
            grade.setStudentName(student.getName());
            grade.setCourseName(course != null ? course.getName() : courseId);
            result.add(grade);
            includedStudentIds.add(student.getStudentId());
        }

        result.sort(Comparator.comparing(Grade::getStudentId, Comparator.nullsLast(String::compareTo)));
        return Result.success(result);
    }

    // 获取成绩详情
    @GetMapping("/{id}")
    public Result<Grade> getById(@PathVariable String id, Authentication authentication) {
        Grade grade = gradeService.getById(id);
        if (grade != null) {
            studentAccessGuard.verifyStudentAccess(authentication, grade.getStudentId());
        }
        return Result.success(grade);
    }

    // 处理提交
    @PostMapping
    public Result<Void> submit(@RequestBody Grade grade, Authentication authentication) {
        grade.setTeacherId(requireTeacherId(authentication, grade.getTeacherId()));
        gradeService.submitGrade(grade);
        return Result.success("鎴愮哗褰曞叆鎴愬姛", null);
    }

    // 批量提交
    @PostMapping("/batch")
    public Result<Void> batchSubmit(@RequestBody List<Grade> grades, Authentication authentication) {
        if (grades == null || grades.isEmpty()) {
            return Result.badRequest("成绩记录不能为空");
        }
        for (Grade grade : grades) {
            grade.setTeacherId(requireTeacherId(authentication, grade.getTeacherId()));
        }
        gradeService.batchSubmitGrades(grades);
        return Result.success("鎵归噺褰曞叆鎴愬姛", null);
    }

    // 处理通过
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable String id) {
        gradeService.approveGrade(id);
        return Result.success("瀹℃牳閫氳繃", null);
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable String id, @RequestBody(required = false) Map<String, String> params) {
        String reason = params != null ? params.get("reason") : null;
        gradeService.rejectGrade(id, reason);
        return Result.success("宸查┏鍥?", null);
    }

    // 更新成绩
    @PutMapping
    public Result<Void> update(@RequestBody Grade grade) {
        if (!"rejected".equals(grade.getStatus())) {
            return Result.badRequest("鍙兘淇敼宸查┏鍥炵殑鎴愮哗");
        }
        grade.setStatus("submitted");
        grade.setIsPass(null);
        gradeService.updateById(grade);
        return Result.success("淇敼鎴愬姛锛屽凡閲嶆柊鎻愪氦瀹℃牳", null);
    }

    // 处理绩点
    @GetMapping("/gpa/{studentId}")
    public Result<Map<String, Object>> gpa(
            @PathVariable String studentId,
            @RequestParam(required = false) String semesterId,
            Authentication authentication) {
        studentAccessGuard.verifyStudentAccess(authentication, studentId);
        return Result.success(gradeService.calculateGpa(studentId, semesterId));
    }

    // 处理统计
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String classId) {
        return Result.success(gradeService.getStatistics(semesterId, courseId, classId));
    }

    // 补全成绩信息
    private void enrichGrades(List<Grade> grades) {
        if (grades == null || grades.isEmpty()) {
            return;
        }

        Set<String> studentIds = grades.stream()
                .map(Grade::getStudentId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());
        Set<String> courseIds = grades.stream()
                .map(Grade::getCourseId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());

        Map<String, Student> studentMap = studentIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getStudentId, Function.identity(), (left, right) -> left));
        Map<String, Course> courseMap = courseIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(), (left, right) -> left));

        for (Grade grade : grades) {
            Student student = studentMap.get(grade.getStudentId());
            if (student != null) {
                grade.setStudentName(student.getName());
            }
            Course course = courseMap.get(grade.getCourseId());
            if (course != null) {
                grade.setCourseName(course.getName());
                grade.setCredits(course.getCredits());
            }
            if (grade.getTotalScore() != null) {
                double point = grade.getTotalScore() >= 60 ? (grade.getTotalScore() - 50) / 10 : 0;
                grade.setGradePoint(Math.round(point * 100) / 100.0);
            }
        }
    }

    // 选取最新成绩记录
    private Grade pickLatestGrade(Grade left, Grade right) {
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

    private String requireTeacherId(Authentication authentication, String requestedTeacherId) {
        String teacherId = resolveTeacherId(authentication, requestedTeacherId);
        if (teacherId == null || teacherId.trim().isEmpty()) {
            throw new BusinessException(400, "无法确定授课教师，请重新选择课程后提交");
        }
        return teacherId.trim();
    }
}
