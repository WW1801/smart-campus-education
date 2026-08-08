package com.campus.education.mapper;

/**
 * 成绩数据访问接口，负责执行成绩相关持久化操作。
 * 学业预警固定查询仅读取已审核成绩，并按可选学期和学生范围汇总。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.Grade;

public interface GradeMapper extends BaseMapper<Grade> {
}
