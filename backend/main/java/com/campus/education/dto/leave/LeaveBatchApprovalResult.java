package com.campus.education.dto.leave;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 批量请假审批结果，业务失败按申请单保留明细。 */
@Data
public class LeaveBatchApprovalResult {
    private int successCount;
    private int failedCount;
    private List<LeaveApprovalResult> results = new ArrayList<>();
}
