package com.campus.education.service.impl;

/**
 * 课表服务实现类，负责处理课表相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Schedule;
import com.campus.education.mapper.ScheduleMapper;
import com.campus.education.service.ScheduleService;
import org.springframework.stereotype.Service;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {
}
