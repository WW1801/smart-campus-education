package com.campus.education.dto.leave;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 单条请假审批及考勤同步结果。 */
@Data
public class LeaveApprovalResult {
    private String requestId;
    private boolean approved;
    private boolean idempotent;
    private int synchronizedCount;
    private String attendanceSyncStatus;
    private List<LeaveSyncFailure> failureDetails = new ArrayList<>();
}
