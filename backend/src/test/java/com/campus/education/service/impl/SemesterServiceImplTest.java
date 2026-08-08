package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.campus.education.entity.Semester;
import com.campus.education.mapper.SemesterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemesterServiceImplTest {
    private SemesterServiceImpl service;
    private SemesterMapper semesterMapper;

    @BeforeEach
    void setUp() {
        service = new SemesterServiceImpl();
        semesterMapper = mock(SemesterMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", semesterMapper);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(
                Instant.parse("2026-07-30T04:00:00Z"), ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void calculatesStatusesFromShanghaiBusinessDateInsteadOfStoredStatus() {
        when(semesterMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                semester("past", "current", LocalDate.of(2024, 2, 20), LocalDate.of(2024, 6, 30)),
                semester("active", "upcoming", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 30)),
                semester("future", "completed", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 15))));

        List<Semester> result = service.listWithRealtimeStatus();

        assertEquals("completed", result.get(0).getStatus());
        assertEquals("current", result.get(1).getStatus());
        assertEquals("upcoming", result.get(2).getStatus());
    }

    @Test
    void treatsBothDateBoundariesAsCurrent() {
        Semester startsToday = semester("start", "upcoming", LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 30));
        Semester endsToday = semester("end", "completed", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 30));

        assertEquals("current", SemesterServiceImpl.resolveStatus(startsToday, LocalDate.of(2026, 7, 30)));
        assertEquals("current", SemesterServiceImpl.resolveStatus(endsToday, LocalDate.of(2026, 7, 30)));
    }

    @Test
    void keepsConfiguredCurrentSemesterDuringDateGap() {
        when(semesterMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                semester("past", "completed", LocalDate.of(2024, 2, 20), LocalDate.of(2024, 6, 30)),
                semester("next", "current", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 15))));

        List<Semester> result = service.listWithRealtimeStatus();

        assertEquals("completed", result.get(0).getStatus());
        assertEquals("current", result.get(1).getStatus());
    }

    @Test
    void marksMissingOrReversedDateRangesInvalid() {
        Semester missing = semester("missing", "current", null, LocalDate.of(2026, 7, 30));
        Semester reversed = semester("reversed", "current", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 30));

        assertEquals("invalid", SemesterServiceImpl.resolveStatus(missing, LocalDate.of(2026, 7, 30)));
        assertEquals("invalid", SemesterServiceImpl.resolveStatus(reversed, LocalDate.of(2026, 7, 30)));
    }

    private Semester semester(String id, String storedStatus, LocalDate startDate, LocalDate endDate) {
        Semester semester = new Semester();
        semester.setSemesterId(id);
        semester.setStatus(storedStatus);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        return semester;
    }
}
