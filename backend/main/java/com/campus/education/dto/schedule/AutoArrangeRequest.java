package com.campus.education.dto.schedule;

import lombok.Data;

import java.util.List;

/** Request for deterministic, conflict-aware automatic course arrangement. */
@Data
public class AutoArrangeRequest {
    private String semesterId;
    private List<String> classroomIds;
    private List<TimeSlot> timeSlots;
    private List<Task> tasks;

    @Data
    public static class Task {
        private String courseId;
        private String teacherId;
        private String classId;
        private String mode;
        private Integer maxStudents;
    }

    @Data
    public static class TimeSlot {
        private Integer dayOfWeek;
        private Integer startPeriod;
        private Integer endPeriod;
    }
}
