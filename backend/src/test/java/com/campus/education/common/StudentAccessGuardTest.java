package com.campus.education.common;

import com.campus.education.entity.User;
import com.campus.education.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentAccessGuardTest {

    private StudentAccessGuard guard;
    private UserService userService;

    @BeforeEach
    void setUp() {
        guard = new StudentAccessGuard();
        userService = mock(UserService.class);
        ReflectionTestUtils.setField(guard, "userService", userService);
    }

    @Test
    void shouldRejectStudentAccessToAnotherStudent() {
        when(userService.getById("U005")).thenReturn(user("5", "S001"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.currentStudentId(authentication("U005"), "S002"));

        assertEquals(403, exception.getCode());
    }

    @Test
    void shouldForceStudentFilterToBoundStudentId() {
        when(userService.getById("U005")).thenReturn(user("5", "S001"));

        assertEquals("S001", guard.resolveStudentFilter(authentication("U005"), null));
    }

    @Test
    void shouldKeepAdminRequestedStudentFilter() {
        when(userService.getById("U001")).thenReturn(user("1", null));

        assertEquals("S002", guard.resolveStudentFilter(authentication("U001"), "S002"));
    }

    private Authentication authentication(String userId) {
        return new UsernamePasswordAuthenticationToken(userId, null);
    }

    private User user(String roleId, String relatedId) {
        User user = new User();
        user.setUserId("U");
        user.setRoleId(roleId);
        user.setRelatedId(relatedId);
        return user;
    }
}
