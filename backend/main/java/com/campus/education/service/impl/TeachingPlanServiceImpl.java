package com.campus.education.service.impl;

/**
 * 教学计划服务实现类，负责处理教学计划相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.TeachingPlan;
import com.campus.education.mapper.TeachingPlanMapper;
import com.campus.education.service.TeachingPlanService;
import org.springframework.stereotype.Service;

@Service
public class TeachingPlanServiceImpl extends ServiceImpl<TeachingPlanMapper, TeachingPlan> implements TeachingPlanService {
}
