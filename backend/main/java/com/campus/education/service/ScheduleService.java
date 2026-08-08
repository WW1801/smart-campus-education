package com.campus.education.service;

/**
 * 课表服务接口，定义课表相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.dto.schedule.AutoArrangeRequest;
import com.campus.education.entity.Schedule;

import java.util.Map;

public interface ScheduleService extends IService<Schedule> {
    Map<String, Object> autoArrange(AutoArrangeRequest request);
}
