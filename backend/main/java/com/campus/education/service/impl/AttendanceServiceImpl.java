package com.campus.education.service.impl;

/**
 * 考勤服务实现类，负责处理考勤相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Attendance;
import com.campus.education.mapper.AttendanceMapper;
import com.campus.education.service.AttendanceService;
import org.springframework.stereotype.Service;

@Service
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance> implements AttendanceService {
}
