package com.campus.education.service;

import com.campus.education.common.BusinessIdGenerator;
import com.campus.education.entity.Student;
import com.campus.education.entity.Teacher;
import com.campus.education.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountProvisioningServiceTest {

    @Test
    void shouldProvisionTeacherWithTeacherNumberAndInitialPassword() {
        UserService userService = mock(UserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        BusinessIdGenerator idGenerator = mock(BusinessIdGenerator.class);
        when(userService.findByUsername("T010")).thenReturn(null);
        when(idGenerator.nextNumericId("user", "user_id")).thenReturn("20");
        when(passwordEncoder.encode("123456")).thenReturn("encoded-initial-password");
        when(userService.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(true);
        Teacher teacher = new Teacher();
        teacher.setTeacherId("T010");
        teacher.setName("赵老师");

        new AccountProvisioningService(userService, passwordEncoder, idGenerator).provisionTeacherAccount(teacher);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userService).save(saved.capture());
        assertEquals("T010", saved.getValue().getUsername());
        assertEquals("赵老师", saved.getValue().getName());
        assertEquals("4", saved.getValue().getRoleId());
        assertEquals("T010", saved.getValue().getRelatedId());
        assertEquals("encoded-initial-password", saved.getValue().getPassword());
    }

    @Test
    void shouldProvisionStudentWithStudentNumberAndInitialPassword() {
        UserService userService = mock(UserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        BusinessIdGenerator idGenerator = mock(BusinessIdGenerator.class);
        when(userService.findByUsername("2026001001001")).thenReturn(null);
        when(idGenerator.nextNumericId("user", "user_id")).thenReturn("21");
        when(passwordEncoder.encode("123456")).thenReturn("encoded-initial-password");
        when(userService.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(true);
        Student student = new Student();
        student.setStudentId("S020");
        student.setStudentNo("2026001001001");
        student.setName("新同学");

        new AccountProvisioningService(userService, passwordEncoder, idGenerator).provisionStudentAccount(student);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userService).save(saved.capture());
        assertEquals("2026001001001", saved.getValue().getUsername());
        assertEquals("5", saved.getValue().getRoleId());
        assertEquals("S020", saved.getValue().getRelatedId());
    }
}
