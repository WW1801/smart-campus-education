package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单个学期的固定成绩统计值。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterGradeTrendDTO {
    private String semesterId;
    private Double averageScore;
    private Integer failedCourseCount;
    private Integer courseCount;
}
