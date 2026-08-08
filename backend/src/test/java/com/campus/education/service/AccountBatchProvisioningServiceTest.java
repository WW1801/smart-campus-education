package com.campus.education.service;

import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountBatchProvisioningServiceTest {
    @Test
    void shouldSkipExistingAccountAndProvisionOnlyMissingAccount() {
        TeacherService teachers = mock(TeacherService.class); StudentService students = mock(StudentService.class);
        UserService users = mock(UserService.class); AccountProvisioningService provisioning = mock(AccountProvisioningService.class);
        Teacher missing = new Teacher(); missing.setTeacherId("T001"); missing.setName("张老师");
        Teacher existing = new Teacher(); existing.setTeacherId("T002"); existing.setName("李老师");
        when(teachers.list(org.mockito.ArgumentMatchers.<Wrapper<Teacher>>any())).thenReturn(java.util.Arrays.asList(missing, existing));
        User account = new User(); when(users.findByUsername("T002")).thenReturn(account);
        Map<String, Object> result = new AccountBatchProvisioningService(teachers, students, users, provisioning)
                .provision("teacher", null, null);
        assertEquals(1, result.get("successCount")); assertEquals(0, result.get("failedCount"));
        verify(provisioning).provisionTeacherAccount(missing); verify(provisioning, never()).provisionTeacherAccount(existing);
    }
}
