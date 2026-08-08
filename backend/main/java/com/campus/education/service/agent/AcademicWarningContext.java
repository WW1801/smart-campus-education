package com.campus.education.service.agent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcademicWarningContext {

    private int failedCourseCount;

    private int absentCount;

    private int lateCount;

    private String graduationAuditStatus;

    public boolean hasFailedCourses() {
        return failedCourseCount > 0;
    }

    public boolean hasAttendanceRisk() {
        return absentCount >= 3;
    }

    public boolean isGraduationRejected() {
        return "rejected".equals(graduationAuditStatus);
    }
}
