package com.campus.education.mapper;

/**
 * 审计日志数据访问接口，负责执行审计日志相关持久化操作。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
