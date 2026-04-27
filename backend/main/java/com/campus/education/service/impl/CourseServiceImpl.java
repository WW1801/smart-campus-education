package com.campus.education.service.impl;

/**
 * 课程服务实现类，负责处理课程相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Course;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.service.CourseService;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {
}
