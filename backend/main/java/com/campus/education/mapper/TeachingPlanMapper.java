package com.campus.education.mapper;

/**
 * 教学计划数据访问接口，负责执行教学计划相关持久化操作。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.TeachingPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeachingPlanMapper extends BaseMapper<TeachingPlan> {
}
