package com.campus.education.controller.agent;

import com.campus.education.common.Result;
import com.campus.education.common.GlobalExceptionHandler;
import com.campus.education.dto.agent.AcademicWarningDetailDTO;
import com.campus.education.dto.agent.AcademicWarningRecordDTO;
import com.campus.education.service.EducationAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EducationAgentControllerTest {

    @Test
    void shouldReturnUniformResultForWarningDetail() {
        EducationAgentService service = mock(EducationAgentService.class);
        EducationAgentController controller = new EducationAgentController();
        ReflectionTestUtils.setField(controller, "educationAgentService", service);
        AcademicWarningDetailDTO detail = AcademicWarningDetailDTO.builder()
                .studentId("S001").studentName("测试学生").semesterId("SEM001")
                .triggeredRules(Collections.<String>emptyList()).build();
        when(service.getAcademicWarningDetail("S001", "SEM001")).thenReturn(detail);

        // 直接覆盖路径变量与可选学期参数到统一 Result 的字段映射。
        Result<AcademicWarningDetailDTO> result = controller.warningDetail("S001", "SEM001");

        assertEquals(200, result.getCode());
        assertSame(detail, result.getData());
        verify(service).getAcademicWarningDetail("S001", "SEM001");
    }

    @Test
    void shouldReturnUniformErrorForUnexpectedHistoryFailure() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<Void> result = handler.handleException(new RuntimeException("历史表不可用"));

        assertEquals(500, result.getCode());
        assertNull(result.getData());
    }
}
