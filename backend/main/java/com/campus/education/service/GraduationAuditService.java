package com.campus.education.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.dto.GraduationAuditVO;
import com.campus.education.entity.GraduationAudit;

import java.util.List;
import java.util.Map;

public interface GraduationAuditService extends IService<GraduationAudit> {
    GraduationAuditVO auditStudent(String studentId, String auditOpinion);

    GraduationAuditVO getAuditDetail(String studentId);

    List<GraduationAuditVO> batchAudit(String majorId, String classId);

    GraduationAuditVO grantDegree(String studentId, String auditOpinion);

    IPage<GraduationAuditVO> pageAudits(Integer current, Integer size, String studentId, String majorId, String classId, String status);

    Map<String, Object> getStatistics(String majorId, String classId);
}
