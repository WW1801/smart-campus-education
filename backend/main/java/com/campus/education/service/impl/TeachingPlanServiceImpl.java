package com.campus.education.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.TeachingPlan;
import com.campus.education.mapper.TeachingPlanMapper;
import com.campus.education.service.TeachingPlanService;
import org.springframework.stereotype.Service;

@Service
public class TeachingPlanServiceImpl extends ServiceImpl<TeachingPlanMapper, TeachingPlan> implements TeachingPlanService {
}
