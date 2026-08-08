package com.campus.education.dto.leave;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/** 管理员批量审批请求。 */
@Data
public class LeaveRequestBatchApproveRequest {
    @NotEmpty(message = "请至少选择一条请假申请")
    @Size(max = 100, message = "单次最多审批100条申请")
    private List<String> requestIds;
    @Size(max = 1000, message = "审批意见不能超过1000字")
    private String processOpinion;
}
