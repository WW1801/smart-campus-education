package com.campus.education.controller;

import com.campus.education.common.Result;
import com.campus.education.entity.User;
import com.campus.education.mapper.ClassMapper;
import com.campus.education.mapper.DepartmentMapper;
import com.campus.education.mapper.MajorMapper;
import com.campus.education.mapper.RoleMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    @Test
    void shouldChangePasswordAfterVerifyingCurrentPassword() {
        UserService userService = mock(UserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = new User();
        user.setUserId("5");
        user.setPassword("old-hash");
        when(userService.getById("5")).thenReturn(user);
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");
        AccountController controller = controller(userService, passwordEncoder);
        Map<String, String> request = new HashMap<>();
        request.put("oldPassword", "old-password");
        request.put("newPassword", "newPassword1");

        Result<Void> result = controller.changePassword(
                new UsernamePasswordAuthenticationToken("5", null), request);

        assertEquals(200, result.getCode());
        assertEquals("new-hash", user.getPassword());
        verify(userService).updateById(user);
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {
        UserService userService = mock(UserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = new User();
        user.setUserId("5");
        user.setPassword("old-hash");
        when(userService.getById("5")).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);
        AccountController controller = controller(userService, passwordEncoder);
        Map<String, String> request = new HashMap<>();
        request.put("oldPassword", "wrong-password");
        request.put("newPassword", "newPassword1");

        Result<Void> result = controller.changePassword(
                new UsernamePasswordAuthenticationToken("5", null), request);

        assertEquals(400, result.getCode());
    }

    private AccountController controller(UserService userService, PasswordEncoder passwordEncoder) {
        return new AccountController(
                userService,
                passwordEncoder,
                mock(RoleMapper.class),
                mock(StudentMapper.class),
                mock(TeacherMapper.class),
                mock(DepartmentMapper.class),
                mock(MajorMapper.class),
                mock(ClassMapper.class));
    }
}
