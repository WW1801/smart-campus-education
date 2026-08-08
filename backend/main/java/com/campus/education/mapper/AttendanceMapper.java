package com.campus.education.mapper;

/**
 * 考勤数据访问接口，负责执行考勤相关持久化操作。
 * 学业预警固定查询按可选学期和学生范围读取考勤，缺勤规则仅统计 absent 状态。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.Attendance;

public interface AttendanceMapper extends BaseMapper<Attendance> {
}
