package com.campus.education.service.impl;

/**
 * 院系服务实现类，负责处理院系相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Department;
import com.campus.education.mapper.DepartmentMapper;
import com.campus.education.service.DepartmentService;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {
}
