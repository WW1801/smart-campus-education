package com.campus.education.controller.graduation;

/**
 * 毕业审核控制器，负责处理毕业审核相关接口请求。
 */

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.education.common.Result;
import com.campus.education.dto.GraduationAuditVO;
import com.campus.education.service.GraduationAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/graduation")
public class GraduationAuditController {

    @Autowired
    private GraduationAuditService graduationAuditService;

    // 处理审核学生
    @PostMapping("/audit/{studentId}")
    public Result<GraduationAuditVO> auditStudent(@PathVariable String studentId,
                                                  @RequestBody(required = false) Map<String, String> params) {
        String opinion = params != null ? params.get("auditOpinion") : null;
        return Result.success("毕业审核完成", graduationAuditService.auditStudent(studentId, opinion));
    }

    // 获取审核详情
    @GetMapping("/audit/{studentId}")
    public Result<GraduationAuditVO> getAuditDetail(@PathVariable String studentId) {
        return Result.success(graduationAuditService.getAuditDetail(studentId));
    }

    @PostMapping("/batch-audit")
    public Result<List<GraduationAuditVO>> batchAudit(@RequestBody(required = false) Map<String, String> params) {
        String majorId = params != null ? params.get("majorId") : null;
        String classId = params != null ? params.get("classId") : null;
        return Result.success("批量审核完成", graduationAuditService.batchAudit(majorId, classId));
    }

    // 授予学位
    @PutMapping("/degree/{studentId}")
    public Result<GraduationAuditVO> grantDegree(@PathVariable String studentId,
                                                 @RequestBody(required = false) Map<String, String> params) {
        String opinion = params != null ? params.get("auditOpinion") : null;
        return Result.success("学位授予完成", graduationAuditService.grantDegree(studentId, opinion));
    }

    @GetMapping("/page")
    public Result<IPage<GraduationAuditVO>> page(@RequestParam(defaultValue = "1") Integer current,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String studentId,
                                                 @RequestParam(required = false) String majorId,
                                                 @RequestParam(required = false) String classId,
                                                 @RequestParam(required = false) String status) {
        return Result.success(graduationAuditService.pageAudits(current, size, studentId, majorId, classId, status));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(required = false) String majorId,
                                                  @RequestParam(required = false) String classId) {
        return Result.success(graduationAuditService.getStatistics(majorId, classId));
    }

    // 获取补修课程
    @GetMapping("/remedial/{studentId}")
    public Result<Map<String, Object>> remedialCourses(@PathVariable String studentId) {
        return Result.success(graduationAuditService.getRemedialCourses(studentId));
    }
}
