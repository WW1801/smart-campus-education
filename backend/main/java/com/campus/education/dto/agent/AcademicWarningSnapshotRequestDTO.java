package com.campus.education.dto.agent;

import lombok.Data;

/**
 * 批量保存当前筛选范围内学业预警快照的请求对象。
 */
@Data
public class AcademicWarningSnapshotRequestDTO {
    private String semesterId;
    private String departmentId;
    private String majorId;
    private String classId;
    private String keyword;
    private String level;
}
