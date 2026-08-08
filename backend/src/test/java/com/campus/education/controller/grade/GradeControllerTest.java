package com.campus.education.controller.grade;

import com.campus.education.common.Result;
import com.campus.education.entity.Grade;
import com.campus.education.entity.User;
import com.campus.education.service.GradeService;
import com.campus.education.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GradeControllerTest {
    private GradeController controller;
    private GradeService gradeService;
    private UserService userService;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new GradeController();
        gradeService = mock(GradeService.class);
        userService = mock(UserService.class);
        authentication = mock(Authentication.class);
        ReflectionTestUtils.setField(controller, "gradeService", gradeService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        when(authentication.getName()).thenReturn("U1");
    }

    @Test
    void teacherSubmissionAlwaysUsesAuthenticatedTeacherId() {
        User teacher = user("4", "T15");
        when(userService.getById("U1")).thenReturn(teacher);
        Grade grade = new Grade();
        grade.setTeacherId("T99");

        controller.submit(grade, authentication);

        assertEquals("T15", grade.getTeacherId());
        verify(gradeService).submitGrade(grade);
    }

    @Test
    void administratorSubmissionKeepsSelectedScheduleTeacher() {
        when(userService.getById("U1")).thenReturn(user("1", null));
        Grade grade = new Grade();
        grade.setTeacherId("T15");

        controller.submit(grade, authentication);

        assertEquals("T15", grade.getTeacherId());
        verify(gradeService).submitGrade(grade);
    }

    @Test
    void emptyBatchIsRejectedWithoutCallingService() {
        Result<Void> result = controller.batchSubmit(Collections.emptyList(), authentication);

        assertEquals(400, result.getCode());
        verify(gradeService, never()).batchSubmitGrades(org.mockito.ArgumentMatchers.anyList());
    }

    private User user(String roleId, String relatedId) {
        User user = new User();
        user.setRoleId(roleId);
        user.setRelatedId(relatedId);
        return user;
    }
}
