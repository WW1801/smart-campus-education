package com.campus.education.mapper;

/**
 * 学生选课数据访问接口，负责执行学生选课相关持久化操作。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.StudentCourseSelection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentCourseSelectionMapper extends BaseMapper<StudentCourseSelection> {
}
