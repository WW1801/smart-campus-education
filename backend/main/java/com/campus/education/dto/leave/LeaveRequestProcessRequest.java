package com.campus.education.dto.leave;

import lombok.Data;

import javax.validation.constraints.Size;

/** 管理员审批意见。 */
@Data
public class LeaveRequestProcessRequest {
    @Size(max = 1000, message = "审批意见不能超过1000字")
    private String processOpinion;
}
