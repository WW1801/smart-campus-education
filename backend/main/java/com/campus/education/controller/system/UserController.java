package com.campus.education.controller.system;

/**
 * 用户控制器，负责处理用户相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessIdGenerator;
import com.campus.education.common.Result;
import com.campus.education.entity.Student;
import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.UserService;
import com.campus.education.service.AccountProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class UserController {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BusinessIdGenerator businessIdGenerator;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    // 分页查询用户
    @GetMapping("/page")
    public Result<IPage<User>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String roleId) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.trim().isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (roleId != null && !roleId.trim().isEmpty()) {
            wrapper.eq(User::getRoleId, roleId);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        IPage<User> result = userService.page(page, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    // 查询用户列表
    @GetMapping("/list")
    public Result<List<User>> list() {
        List<User> users = userService.list();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    // 获取用户详情
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable String id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    // 添加用户
    @PostMapping
    public Result<Map<String, String>> add(@RequestBody User user) {
        String accountError = synchronizeBusinessAccount(user);
        if (accountError != null) {
            return Result.badRequest(accountError);
        }
        User existing = userService.findByUsername(user.getUsername());
        if (existing != null) {
            return Result.badRequest("用户名已存在");
        }
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            user.setUserId(businessIdGenerator.nextNumericId("user", "user_id"));
        }
        String rawPassword = user.getPassword();
        boolean generated = rawPassword == null || rawPassword.trim().isEmpty();
        if (generated) {
            rawPassword = generateTemporaryPassword();
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userService.save(user);
        if (generated) {
            return Result.success("添加成功，已生成临时密码", passwordResult(rawPassword));
        }
        return Result.success("添加成功", null);
    }

    // 更新用户
    @PutMapping
    public Result<Void> update(@RequestBody User user) {
        User existing = userService.getById(user.getUserId());
        if (existing == null) {
            return Result.badRequest("用户不存在");
        }
        if (user.getRoleId() == null || user.getRoleId().trim().isEmpty()) {
            user.setRoleId(existing.getRoleId());
        }
        if (user.getRelatedId() == null) {
            user.setRelatedId(existing.getRelatedId());
        }
        String accountError = synchronizeBusinessAccount(user);
        if (accountError != null) {
            return Result.badRequest(accountError);
        }
        User usernameOwner = userService.findByUsername(user.getUsername());
        if (usernameOwner != null && !usernameOwner.getUserId().equals(user.getUserId())) {
            return Result.badRequest("登录账号已存在，请检查关联的学生或教师信息");
        }
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(existing.getPassword());
        }
        userService.updateById(user);
        return Result.success("更新成功", null);
    }

    // 删除用户
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        userService.removeById(id);
        return Result.success("删除成功", null);
    }

    // 重置密码
    @PutMapping("/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable String id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.badRequest("用户不存在");
        }
        String resetPassword = AccountProvisioningService.INITIAL_PASSWORD;
        user.setPassword(passwordEncoder.encode(resetPassword));
        userService.updateById(user);
        return Result.success("密码已重置为系统初始密码", null);
    }

    // 处理生成临时密码
    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    // 处理密码结果
    private Map<String, String> passwordResult(String password) {
        Map<String, String> data = new HashMap<>();
        data.put("temporaryPassword", password);
        return data;
    }

    /**
     * 教师与学生的登录账号属于业务标识，不允许由前端自行编辑，以免账号与人员信息脱节。
     */
    private String synchronizeBusinessAccount(User user) {
        if ("4".equals(user.getRoleId())) {
            if (user.getRelatedId() == null || user.getRelatedId().trim().isEmpty()) {
                return "教师账号必须关联有效教师，请填写教师工号";
            }
            Teacher teacher = teacherMapper.selectById(user.getRelatedId());
            if (teacher == null) {
                return "未找到关联的教师，请确认工号后再保存";
            }
            user.setUsername(teacher.getTeacherId());
            user.setName(teacher.getName());
        } else if ("5".equals(user.getRoleId())) {
            if (user.getRelatedId() == null || user.getRelatedId().trim().isEmpty()) {
                return "学生账号必须关联有效学生，请填写学生ID";
            }
            Student student = studentMapper.selectById(user.getRelatedId());
            if (student == null) {
                return "未找到关联的学生，请确认学生ID后再保存";
            }
            if (student.getStudentNo() == null || student.getStudentNo().trim().isEmpty()) {
                return "关联学生尚未生成学号，请先完善学生信息";
            }
            user.setUsername(student.getStudentNo());
            user.setName(student.getName());
        }
        return null;
    }
}
