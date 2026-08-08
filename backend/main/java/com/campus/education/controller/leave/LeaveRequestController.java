package com.campus.education.controller.leave;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.common.StudentAccessGuard;
import com.campus.education.dto.leave.LeaveApprovalResult;
import com.campus.education.dto.leave.LeaveBatchApprovalResult;
import com.campus.education.dto.leave.LeaveRequestBatchApproveRequest;
import com.campus.education.dto.leave.LeaveRequestCreateRequest;
import com.campus.education.dto.leave.LeaveRequestProcessRequest;
import com.campus.education.entity.LeaveRequest;
import com.campus.education.service.LeaveRequestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/leave-requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;
    private final StudentAccessGuard studentAccessGuard;

    public LeaveRequestController(LeaveRequestService leaveRequestService, StudentAccessGuard studentAccessGuard) {
        this.leaveRequestService = leaveRequestService;
        this.studentAccessGuard = studentAccessGuard;
    }

    @PostMapping
    public Result<LeaveRequest> create(@Valid @RequestBody LeaveRequestCreateRequest request,
                                       Authentication authentication) {
        String studentId = studentAccessGuard.currentStudentId(authentication, request.getStudentId());
        return Result.success("请假申请已提交", leaveRequestService.create(request, studentId));
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> my(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long limit,
                                          @RequestParam(required = false) String status,
                                          Authentication authentication) {
        String studentId = studentAccessGuard.currentStudentId(authentication, null);
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, studentId)
                .eq(notBlank(status), LeaveRequest::getStatus, status)
                .orderByDesc(LeaveRequest::getSubmittedAt);
        return Result.success("查询成功", pageData(page, limit, wrapper));
    }

    @PutMapping("/{requestId}/cancel")
    public Result<LeaveRequest> cancel(@PathVariable String requestId, Authentication authentication) {
        String studentId = studentAccessGuard.currentStudentId(authentication, null);
        return Result.success("请假申请已撤销", leaveRequestService.cancel(requestId, studentId));
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long limit,
                                            @RequestParam(required = false) String studentId,
                                            @RequestParam(required = false) String courseId,
                                            @RequestParam(required = false) String semesterId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<LeaveRequest>()
                .eq(notBlank(studentId), LeaveRequest::getStudentId, studentId)
                .eq(notBlank(courseId), LeaveRequest::getCourseId, courseId)
                .eq(notBlank(semesterId), LeaveRequest::getSemesterId, semesterId)
                .eq(notBlank(status), LeaveRequest::getStatus, status)
                .le(endDate != null, LeaveRequest::getStartDate, endDate)
                .ge(startDate != null, LeaveRequest::getEndDate, startDate)
                .orderByDesc(LeaveRequest::getSubmittedAt);
        return Result.success("查询成功", pageData(page, limit, wrapper));
    }

    @PutMapping("/{requestId}/approve")
    public Result<LeaveApprovalResult> approve(@PathVariable String requestId,
                                                @Valid @RequestBody(required = false) LeaveRequestProcessRequest body,
                                                Authentication authentication) {
        LeaveApprovalResult result = leaveRequestService.approve(requestId, authentication.getName(), opinion(body));
        if (!result.isApproved()) return Result.error(409, "审批未完成，存在考勤同步冲突", result);
        return Result.success(result.isIdempotent() ? "申请已审批，无需重复处理" : "审批通过并已同步考勤", result);
    }

    @PutMapping("/{requestId}/reject")
    public Result<LeaveRequest> reject(@PathVariable String requestId,
                                       @Valid @RequestBody(required = false) LeaveRequestProcessRequest body,
                                       Authentication authentication) {
        return Result.success("请假申请已拒绝",
                leaveRequestService.reject(requestId, authentication.getName(), opinion(body)));
    }

    @PutMapping("/batch-approve")
    public Result<LeaveBatchApprovalResult> batchApprove(@Valid @RequestBody LeaveRequestBatchApproveRequest body,
                                                          Authentication authentication) {
        LeaveBatchApprovalResult result = leaveRequestService.batchApprove(
                body.getRequestIds(), authentication.getName(), body.getProcessOpinion());
        return Result.success(result.getFailedCount() == 0 ? "批量审批完成" : "批量审批部分完成", result);
    }

    private Map<String, Object> pageData(long page, long limit, LambdaQueryWrapper<LeaveRequest> wrapper) {
        long safePage = Math.max(1, page);
        long safeLimit = Math.min(100, Math.max(1, limit));
        IPage<LeaveRequest> result = leaveRequestService.page(new Page<>(safePage, safeLimit), wrapper);
        leaveRequestService.enrich(result.getRecords());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    private String opinion(LeaveRequestProcessRequest body) {
        return body == null ? null : body.getProcessOpinion();
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
