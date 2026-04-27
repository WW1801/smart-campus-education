package com.campus.education.service;

/**
 * 毕业审核服务接口，定义毕业审核相关业务能力。
 */

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.dto.GraduationAuditVO;
import com.campus.education.entity.GraduationAudit;

import java.util.List;
import java.util.Map;

public interface GraduationAuditService extends IService<GraduationAudit> {
    // 处理审核学生
    GraduationAuditVO auditStudent(String studentId, String auditOpinion);

    // 获取审核详情
    GraduationAuditVO getAuditDetail(String studentId);

    // 批量审核
    List<GraduationAuditVO> batchAudit(String majorId, String classId);

    // 授予学位
    GraduationAuditVO grantDegree(String studentId, String auditOpinion);

    // 分页查询审核记录
    IPage<GraduationAuditVO> pageAudits(Integer current, Integer size, String studentId, String majorId, String classId, String status);

    // 获取统计
    Map<String, Object> getStatistics(String majorId, String classId);

    // 获取补修课程
    Map<String, Object> getRemedialCourses(String studentId);
}
