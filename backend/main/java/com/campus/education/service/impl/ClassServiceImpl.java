package com.campus.education.service.impl;

/**
 * 班级服务实现类，负责处理班级相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Class;
import com.campus.education.mapper.ClassMapper;
import com.campus.education.service.ClassService;
import org.springframework.stereotype.Service;

@Service
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {
}
