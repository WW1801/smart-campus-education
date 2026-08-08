package com.campus.education.dto.leave;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/** 学生提交请假申请的请求参数；studentId 仅用于检测越权，courseId 可为空表示不关联具体课程。 */
@Data
public class LeaveRequestCreateRequest {
    private String studentId;
    private String courseId;
    @NotBlank(message = "学期不能为空")
    private String semesterId;
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
    @NotBlank(message = "请假类型不能为空")
    private String leaveType;
    @NotBlank(message = "请假原因不能为空")
    @Size(max = 1000, message = "请假原因不能超过1000字")
    private String reason;
}
