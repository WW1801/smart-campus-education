package com.campus.education.service.impl;

/**
 * 教室服务实现类，负责处理教室相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Classroom;
import com.campus.education.mapper.ClassroomMapper;
import com.campus.education.service.ClassroomService;
import org.springframework.stereotype.Service;

@Service
public class ClassroomServiceImpl extends ServiceImpl<ClassroomMapper, Classroom> implements ClassroomService {
}
