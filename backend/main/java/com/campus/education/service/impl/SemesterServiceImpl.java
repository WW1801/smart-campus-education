package com.campus.education.service.impl;

/**
 * 学期服务实现类，负责处理学期相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Semester;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.service.SemesterService;
import org.springframework.stereotype.Service;

@Service
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, Semester> implements SemesterService {
}
