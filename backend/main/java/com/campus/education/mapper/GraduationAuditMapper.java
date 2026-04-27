package com.campus.education.mapper;

/**
 * 毕业审核数据访问接口，负责执行毕业审核相关持久化操作。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.GraduationAudit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GraduationAuditMapper extends BaseMapper<GraduationAudit> {
}
