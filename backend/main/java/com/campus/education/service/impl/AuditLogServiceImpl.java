package com.campus.education.service.impl;

/**
 * 审计日志服务实现类，负责处理审计日志相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.AuditLog;
import com.campus.education.mapper.AuditLogMapper;
import com.campus.education.service.AuditLogService;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {
}
