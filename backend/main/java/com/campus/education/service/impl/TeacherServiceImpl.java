package com.campus.education.service.impl;

/**
 * 教师服务实现类，负责处理教师相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Teacher;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.TeacherService;
import org.springframework.stereotype.Service;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {
}
