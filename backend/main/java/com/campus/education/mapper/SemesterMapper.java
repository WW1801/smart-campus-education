package com.campus.education.mapper;

/**
 * 学期数据访问接口，负责执行学期相关持久化操作。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.Semester;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SemesterMapper extends BaseMapper<Semester> {
    /** Serializes automatic arrangement per semester to avoid concurrent conflict checks racing. */
    @Select("SELECT semester_id FROM semester WHERE semester_id = #{semesterId} FOR UPDATE")
    String lockById(@Param("semesterId") String semesterId);
}
