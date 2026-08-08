package com.campus.education.controller.student;

/**
 * 学生控制器，负责处理学生相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.common.StudentAccessGuard;
import com.campus.education.common.StudentNoResolver;
import com.campus.education.entity.Student;
import com.campus.education.service.AccountProvisioningService;
import com.campus.education.service.StudentService;
import com.campus.education.service.PersonnelLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AccountProvisioningService accountProvisioningService;

    @Autowired
    private StudentAccessGuard studentAccessGuard;

    @Autowired
    private PersonnelLifecycleService personnelLifecycleService;

    // 查询学生列表
    @GetMapping("/list")
    public Result<List<Student>> list(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (classId != null && !classId.trim().isEmpty()) {
            wrapper.eq(Student::getClassId, classId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Student::getStatus, status);
        }
        wrapper.orderByAsc(Student::getStudentId);
        return Result.success(studentService.list(wrapper));
    }

    // 分页查询学生
    @GetMapping("/page")
    public Result<IPage<Student>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String majorId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status) {
        Page<Student> page = new Page<>(current, size);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null && !studentId.trim().isEmpty()) {
            wrapper.like(Student::getStudentId, studentId);
        }
        // 对外学号与内部主键分离，列表默认按业务学号检索。
        // 历史数据尚未迁移时，业务学号只存在于展示层；因此不能在 SQL 中先过滤。
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Student::getName, name);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Student::getDepartmentId, departmentId);
        }
        if (majorId != null && !majorId.trim().isEmpty()) {
            wrapper.eq(Student::getMajorId, majorId);
        }
        if (classId != null && !classId.trim().isEmpty()) {
            wrapper.eq(Student::getClassId, classId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Student::getStatus, status);
        }
        wrapper.orderByDesc(Student::getCreatedAt);
        if (studentNo == null || studentNo.trim().isEmpty()) {
            IPage<Student> result = studentService.page(page, wrapper);
            fillDisplayStudentNos(result.getRecords());
            return Result.success(result);
        }

        List<Student> matched = fillDisplayStudentNos(studentService.list(wrapper)).stream()
                .filter(student -> student.getStudentNo() != null && student.getStudentNo().contains(studentNo.trim()))
                .collect(Collectors.toList());
        long from = Math.max(0L, (long) (current - 1) * size);
        long to = Math.min(from + size, matched.size());
        Page<Student> result = new Page<>(current, size, matched.size());
        result.setRecords(from >= matched.size() ? java.util.Collections.emptyList() : matched.subList((int) from, (int) to));
        return Result.success(result);
    }

    // 获取学生详情
    @GetMapping("/{id}")
    public Result<Student> getById(@PathVariable String id, Authentication authentication) {
        studentAccessGuard.verifyStudentAccess(authentication, id);
        return Result.success(studentService.getById(id));
    }

    // 添加学生
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> add(@RequestBody Student student) {
        studentService.createStudent(student);
        accountProvisioningService.provisionStudentAccount(student);
        return Result.success("新增成功，已自动开通账号；初始密码为123456，请首次登录后及时修改", null);
    }

    // 更新学生
    @PutMapping
    public Result<Void> update(@RequestBody Student student) {
        studentService.updateStudent(student);
        return Result.success("更新成功", null);
    }

    // 删除学生
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        personnelLifecycleService.verifyStudentCanDelete(id);
        studentService.removeById(id);
        return Result.success("删除成功", null);
    }

    // 变更状态
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable String id, @RequestBody Map<String, String> params) {
        String targetStatus = params.get("status");
        String reason = params.get("reason");
        if (targetStatus == null || targetStatus.trim().isEmpty()) {
            return Result.badRequest("目标状态不能为空");
        }
        studentService.changeStatus(id, targetStatus, reason);
        return Result.success("学籍状态变更成功", null);
    }

    /** 仅补全响应对象中的展示学号，GET 请求不会写回 student 表。 */
    private List<Student> fillDisplayStudentNos(List<Student> students) {
        if (students == null || students.isEmpty()) return students;
        if (students.stream().allMatch(StudentNoResolver::isStandard)) return students;
        List<Student> allStudents = studentService.list();
        StudentNoResolver.fillDisplayStudentNos(allStudents);
        Map<String, Student> resolved = allStudents.stream()
                .collect(Collectors.toMap(Student::getStudentId, item -> item, (left, right) -> left));
        return students.stream().map(student -> resolved.getOrDefault(student.getStudentId(), student))
                .collect(Collectors.toList());
    }
}
