package com.campus.education.mapper;

/**
 * 排课数据访问接口，负责执行排课相关持久化操作。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {
}
