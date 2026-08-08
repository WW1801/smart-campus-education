package com.campus.education.controller.system;

import com.campus.education.entity.Semester;
import com.campus.education.service.SemesterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SemesterControllerTest {
    private MockMvc mockMvc;
    private SemesterService semesterService;

    @BeforeEach
    void setUp() {
        SemesterController controller = new SemesterController();
        semesterService = mock(SemesterService.class);
        ReflectionTestUtils.setField(controller, "semesterService", semesterService);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listReturnsRealtimeStatusInUnifiedResponse() throws Exception {
        Semester semester = new Semester();
        semester.setSemesterId("SEM1");
        semester.setName("2026—2027学年第一学期");
        semester.setStartDate(LocalDate.of(2026, 2, 1));
        semester.setEndDate(LocalDate.of(2026, 7, 30));
        semester.setStatus("current");
        when(semesterService.listWithRealtimeStatus()).thenReturn(Collections.singletonList(semester));

        mockMvc.perform(get("/semester/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data[0].semesterId").value("SEM1"))
                .andExpect(jsonPath("$.data[0].name").value("2026—2027学年第一学期"))
                .andExpect(jsonPath("$.data[0].startDate").value("2026-02-01"))
                .andExpect(jsonPath("$.data[0].endDate").value("2026-07-30"))
                .andExpect(jsonPath("$.data[0].status").value("current"));
    }
}
