package com.campus.education.service;

/**
 * 教师服务接口，定义教师相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.Teacher;

public interface TeacherService extends IService<Teacher> {
    /** 在职状态变更统一校验，离职和停用会由安全过滤器即时拦截业务请求。 */
    void changeStatus(String teacherId, String targetStatus);
}
