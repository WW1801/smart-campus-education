package com.campus.education.controller;

import com.campus.education.common.BusinessException;
import com.campus.education.common.Result;
import com.campus.education.entity.Department;
import com.campus.education.entity.Major;
import com.campus.education.entity.Role;
import com.campus.education.entity.Student;
import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import com.campus.education.mapper.ClassMapper;
import com.campus.education.mapper.DepartmentMapper;
import com.campus.education.mapper.MajorMapper;
import com.campus.education.mapper.RoleMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 当前登录用户的账号安全与个人信息接口。 */
@RestController
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoleMapper roleMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final DepartmentMapper departmentMapper;
    private final MajorMapper majorMapper;
    private final ClassMapper classMapper;

    public AccountController(UserService userService,
                             PasswordEncoder passwordEncoder,
                             RoleMapper roleMapper,
                             StudentMapper studentMapper,
                             TeacherMapper teacherMapper,
                             DepartmentMapper departmentMapper,
                             MajorMapper majorMapper,
                             ClassMapper classMapper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.roleMapper = roleMapper;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.departmentMapper = departmentMapper;
        this.majorMapper = majorMapper;
        this.classMapper = classMapper;
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(Authentication authentication) {
        User user = currentUser(authentication);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", safeUser(user));
        data.put("profileType", profileType(user.getRoleId()));
        data.put("profile", profile(user));
        data.put("permissions", permissionCodes(user.getRoleId()));
        return Result.success("查询成功", data);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(Authentication authentication, @RequestBody Map<String, String> params) {
        if (params == null) {
            return Result.badRequest("请输入当前密码和新密码");
        }
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (isBlank(oldPassword) || isBlank(newPassword)) {
            return Result.badRequest("请输入当前密码和新密码");
        }
        if (!isValidNewPassword(newPassword)) {
            return Result.badRequest("新密码需为 8 至 64 位，并同时包含字母和数字");
        }
        if (oldPassword.equals(newPassword)) {
            return Result.badRequest("新密码不能与当前密码相同");
        }
        User user = currentUser(authentication);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.badRequest("当前密码不正确，请重新输入");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        return Result.success("密码修改成功，请妥善保管新密码", null);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        User user = userService.getById(String.valueOf(authentication.getPrincipal()));
        if (user == null) {
            throw new BusinessException(401, "当前账号不存在，请重新登录");
        }
        return user;
    }

    private Map<String, Object> safeUser(User user) {
        Role role = roleMapper.selectById(user.getRoleId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("roleId", user.getRoleId());
        data.put("roleName", role != null ? role.getName() : "");
        data.put("relatedId", user.getRelatedId());
        data.put("lastLogin", user.getLastLogin());
        return data;
    }

    private Map<String, Object> profile(User user) {
        if ("5".equals(user.getRoleId())) {
            return studentProfile(user.getRelatedId());
        }
        if ("4".equals(user.getRoleId())) {
            return teacherProfile(user.getRelatedId());
        }
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("userId", user.getUserId());
        admin.put("username", user.getUsername());
        admin.put("name", user.getName());
        admin.put("roleId", user.getRoleId());
        admin.put("roleName", safeUser(user).get("roleName"));
        admin.put("status", "active");
        return admin;
    }

    private Map<String, Object> studentProfile(String studentId) {
        Student student = isBlank(studentId) ? null : studentMapper.selectById(studentId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (student == null) {
            data.put("status", "unbound");
            return data;
        }
        com.campus.education.entity.Class clazz = classMapper.selectById(student.getClassId());
        Major major = majorMapper.selectById(student.getMajorId());
        Department department = departmentMapper.selectById(student.getDepartmentId());
        data.put("studentId", student.getStudentId());
        data.put("studentNo", student.getStudentNo());
        data.put("name", student.getName());
        data.put("classId", student.getClassId());
        data.put("className", clazz != null ? clazz.getName() : "");
        data.put("majorId", student.getMajorId());
        data.put("majorName", major != null ? major.getName() : "");
        data.put("departmentId", student.getDepartmentId());
        data.put("departmentName", department != null ? department.getName() : "");
        data.put("phone", student.getPhone());
        data.put("email", student.getEmail());
        data.put("status", student.getStatus());
        return data;
    }

    private Map<String, Object> teacherProfile(String teacherId) {
        Teacher teacher = isBlank(teacherId) ? null : teacherMapper.selectById(teacherId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (teacher == null) {
            data.put("status", "unbound");
            return data;
        }
        Department department = departmentMapper.selectById(teacher.getDepartmentId());
        data.put("teacherId", teacher.getTeacherId());
        data.put("name", teacher.getName());
        data.put("departmentId", teacher.getDepartmentId());
        data.put("departmentName", department != null ? department.getName() : "");
        data.put("title", teacher.getTitle());
        data.put("phone", teacher.getPhone());
        data.put("email", teacher.getEmail());
        data.put("specialty", teacher.getSpecialty());
        data.put("status", teacher.getStatus());
        return data;
    }

    private List<String> permissionCodes(String roleId) {
        List<String> permissions = userService.getPermissionCodesByRoleId(roleId);
        return permissions == null ? Collections.emptyList() : permissions;
    }

    private String profileType(String roleId) {
        if ("5".equals(roleId)) return "student";
        if ("4".equals(roleId)) return "teacher";
        return "admin";
    }

    private boolean isValidNewPassword(String value) {
        if (value == null || value.length() < 8 || value.length() > 64) {
            return false;
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
