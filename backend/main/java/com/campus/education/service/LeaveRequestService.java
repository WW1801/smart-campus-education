package com.campus.education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.dto.leave.LeaveApprovalResult;
import com.campus.education.dto.leave.LeaveBatchApprovalResult;
import com.campus.education.dto.leave.LeaveRequestCreateRequest;
import com.campus.education.entity.LeaveRequest;

import java.util.List;

/** 请假业务服务，统一维护申请状态与考勤同步事务。 */
public interface LeaveRequestService extends IService<LeaveRequest> {
    LeaveRequest create(LeaveRequestCreateRequest request, String studentId);

    LeaveRequest cancel(String requestId, String studentId);

    LeaveRequest reject(String requestId, String processorUserId, String processOpinion);

    LeaveApprovalResult approve(String requestId, String processorUserId, String processOpinion);

    LeaveBatchApprovalResult batchApprove(List<String> requestIds, String processorUserId, String processOpinion);

    List<LeaveRequest> enrich(List<LeaveRequest> requests);
}
