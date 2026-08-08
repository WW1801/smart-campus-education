package com.campus.education.dto.agent;

import lombok.Data;

/**
 * 管理员更新学业预警处理状态和处理意见的请求对象。
 */
@Data
public class AcademicWarningProcessUpdateDTO {
    private String processStatus;
    private String processOpinion;
}
