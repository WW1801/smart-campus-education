package com.campus.education.mapper;

/**
 * 学生数据访问接口，负责执行学生相关持久化操作。
 * 学业预警列表通过 selectList 按院系、专业、班级固定范围查询，学号和姓名在 Service 中统一过滤。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.Student;

public interface StudentMapper extends BaseMapper<Student> {
}
