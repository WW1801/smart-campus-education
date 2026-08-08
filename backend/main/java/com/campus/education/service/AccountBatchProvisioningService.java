package com.campus.education.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.common.BusinessException;
import com.campus.education.entity.Student;
import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 批量开通只补齐缺失账号；单条业务数据异常不影响其他可开通人员。 */
@Service
public class AccountBatchProvisioningService {
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final UserService userService;
    private final AccountProvisioningService provisioningService;

    public AccountBatchProvisioningService(TeacherService teacherService, StudentService studentService,
                                           UserService userService, AccountProvisioningService provisioningService) {
        this.teacherService = teacherService;
        this.studentService = studentService;
        this.userService = userService;
        this.provisioningService = provisioningService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> provision(String personType, String departmentId, String classId) {
        if (!"teacher".equals(personType) && !"student".equals(personType)) {
            throw new BusinessException("人员类型仅支持 teacher（教师）或 student（学生）。");
        }
        List<Map<String, String>> success = new ArrayList<>();
        List<Map<String, String>> failure = new ArrayList<>();
        if ("teacher".equals(personType)) {
            LambdaQueryWrapper<Teacher> query = new LambdaQueryWrapper<>();
            if (notBlank(departmentId)) query.eq(Teacher::getDepartmentId, departmentId);
            for (Teacher teacher : teacherService.list(query)) {
                provisionTeacher(teacher, success, failure);
            }
        } else {
            LambdaQueryWrapper<Student> query = new LambdaQueryWrapper<>();
            if (notBlank(departmentId)) query.eq(Student::getDepartmentId, departmentId);
            if (notBlank(classId)) query.eq(Student::getClassId, classId);
            for (Student student : studentService.list(query)) {
                provisionStudent(student, success, failure);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success.size());
        result.put("failedCount", failure.size());
        result.put("successDetails", success);
        result.put("failureDetails", failure);
        return result;
    }

    private void provisionTeacher(Teacher teacher, List<Map<String, String>> success, List<Map<String, String>> failure) {
        if (userService.findByUsername(teacher.getTeacherId()) != null) return;
        try { provisioningService.provisionTeacherAccount(teacher); success.add(detail(teacher.getTeacherId(), teacher.getName(), "账号已开通")); }
        catch (BusinessException ex) { failure.add(detail(teacher.getTeacherId(), teacher.getName(), ex.getMessage())); }
    }

    private void provisionStudent(Student student, List<Map<String, String>> success, List<Map<String, String>> failure) {
        if (notBlank(student.getStudentNo()) && userService.findByUsername(student.getStudentNo()) != null) return;
        try { provisioningService.provisionStudentAccount(student); success.add(detail(student.getStudentId(), student.getName(), "账号已开通")); }
        catch (BusinessException ex) { failure.add(detail(student.getStudentId(), student.getName(), ex.getMessage())); }
    }

    private Map<String, String> detail(String personId, String name, String message) {
        Map<String, String> detail = new HashMap<>(); detail.put("personId", personId); detail.put("name", name); detail.put("reason", message);
        detail.put("suggestion", "请核对人员基础信息后重试。"); return detail;
    }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
}
