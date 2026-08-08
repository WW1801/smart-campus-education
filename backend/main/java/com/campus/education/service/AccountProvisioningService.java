package com.campus.education.service;

import com.campus.education.common.BusinessException;
import com.campus.education.common.BusinessIdGenerator;
import com.campus.education.entity.Student;
import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 新增师生时同步开通登录账号，账号标识始终来源于工号或业务学号。
 */
@Service
public class AccountProvisioningService {

    public static final String INITIAL_PASSWORD = "123456";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final BusinessIdGenerator businessIdGenerator;

    public AccountProvisioningService(UserService userService,
                                     PasswordEncoder passwordEncoder,
                                     BusinessIdGenerator businessIdGenerator) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.businessIdGenerator = businessIdGenerator;
    }

    public void provisionTeacherAccount(Teacher teacher) {
        if (teacher == null || isBlank(teacher.getTeacherId()) || isBlank(teacher.getName())) {
            throw new BusinessException("教师工号和姓名不能为空，无法开通登录账号");
        }
        provision(teacher.getTeacherId(), teacher.getName(), "4", teacher.getTeacherId());
    }

    public void provisionStudentAccount(Student student) {
        if (student == null || isBlank(student.getStudentId()) || isBlank(student.getStudentNo()) || isBlank(student.getName())) {
            throw new BusinessException("学生学号和姓名不能为空，无法开通登录账号");
        }
        provision(student.getStudentNo(), student.getName(), "5", student.getStudentId());
    }

    private void provision(String username, String name, String roleId, String relatedId) {
        if (userService.findByUsername(username) != null) {
            throw new BusinessException("登录账号已存在，无法重复开通");
        }
        User user = new User();
        user.setUserId(businessIdGenerator.nextNumericId("user", "user_id"));
        user.setUsername(username);
        user.setName(name);
        user.setRoleId(roleId);
        user.setRelatedId(relatedId);
        user.setPassword(passwordEncoder.encode(INITIAL_PASSWORD));
        if (!userService.save(user)) {
            throw new BusinessException("登录账号开通失败，请稍后重试");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
