package com.campus.education.dto.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** Batch attendance request; row values override common top-level values. */
@Data
public class BatchAttendanceRequest {
    private String courseId;
    private String semesterId;
    private LocalDate date;
    private List<Record> records;

    @Data
    public static class Record {
        private String studentId;
        private String courseId;
        private String semesterId;
        private LocalDate date;
        private String status;
    }
}
