package com.campus.education.service;

/**
 * 学生服务接口，定义学生相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.Student;

public interface StudentService extends IService<Student> {
    // 变更状态
    void changeStatus(String studentId, String targetStatus, String reason);
}
