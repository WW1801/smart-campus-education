package com.campus.education.service;

/**
 * 学期服务接口，定义学期相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.Semester;

import java.util.List;

public interface SemesterService extends IService<Semester> {
    List<Semester> listWithRealtimeStatus();
}
