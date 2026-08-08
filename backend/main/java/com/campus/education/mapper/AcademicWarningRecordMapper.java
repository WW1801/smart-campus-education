package com.campus.education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.AcademicWarningRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学业预警记录 Mapper。
 * 查询用途：分页读取学生的预警快照；关键条件为 student_id，学期、风险等级和处理状态由 Service 可选追加。
 * 使用 MyBatis Plus 通用分页查询，无自定义 XML 或自由拼接 SQL。
 */
@Mapper
public interface AcademicWarningRecordMapper extends BaseMapper<AcademicWarningRecord> {
}
