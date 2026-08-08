package com.campus.education.controller.system;

import com.campus.education.common.Result;
import com.campus.education.entity.Student;
import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.AccountProvisioningService;
import com.campus.education.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserController controller;
    private UserService userService;
    private TeacherMapper teacherMapper;
    private StudentMapper studentMapper;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        controller = new UserController();
        userService = mock(UserService.class);
        teacherMapper = mock(TeacherMapper.class);
        studentMapper = mock(StudentMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "teacherMapper", teacherMapper);
        ReflectionTestUtils.setField(controller, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
    }

    @Test
    void shouldUseTeacherNumberAndNameAsTeacherLoginAccount() {
        Teacher teacher = new Teacher();
        teacher.setTeacherId("T001");
        teacher.setName("张老师");
        when(teacherMapper.selectById("T001")).thenReturn(teacher);
        when(userService.findByUsername("T001")).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");

        User request = account("4", "T001");
        Result<?> result = controller.add(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userService).save(saved.capture());
        assertEquals(200, result.getCode());
        assertEquals("T001", saved.getValue().getUsername());
        assertEquals("张老师", saved.getValue().getName());
        assertEquals("encoded-password", saved.getValue().getPassword());
    }

    @Test
    void shouldUseStudentNumberAndNameAsStudentLoginAccount() {
        Student student = new Student();
        student.setStudentId("S001");
        student.setStudentNo("2021001001001");
        student.setName("李同学");
        when(studentMapper.selectById("S001")).thenReturn(student);
        when(userService.findByUsername("2021001001001")).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");

        User request = account("5", "S001");
        Result<?> result = controller.add(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userService).save(saved.capture());
        assertEquals(200, result.getCode());
        assertEquals("2021001001001", saved.getValue().getUsername());
        assertEquals("李同学", saved.getValue().getName());
    }

    @Test
    void shouldRejectStudentAccountWhenStudentNumberIsMissing() {
        Student student = new Student();
        student.setStudentId("S001");
        student.setName("李同学");
        when(studentMapper.selectById("S001")).thenReturn(student);

        Result<?> result = controller.add(account("5", "S001"));

        assertEquals(400, result.getCode());
        assertNull(result.getData());
        verify(userService, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void shouldResetPasswordToInitialPassword() {
        User existing = new User();
        existing.setUserId("U001");
        existing.setUsername("student-account");
        when(userService.getById("U001")).thenReturn(existing);
        when(passwordEncoder.encode(AccountProvisioningService.INITIAL_PASSWORD)).thenReturn("encoded-initial-password");

        Result<?> result = controller.resetPassword("U001");

        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(updated.capture());
        assertEquals(200, result.getCode());
        assertNull(result.getData());
        assertEquals("encoded-initial-password", updated.getValue().getPassword());
    }

    private User account(String roleId, String relatedId) {
        User user = new User();
        user.setUserId("U001");
        user.setUsername("manual-login-name");
        user.setName("手工姓名");
        user.setPassword("Password123");
        user.setRoleId(roleId);
        user.setRelatedId(relatedId);
        return user;
    }
}
