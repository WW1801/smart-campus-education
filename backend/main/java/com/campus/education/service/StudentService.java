package com.campus.education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.Student;

public interface StudentService extends IService<Student> {
    void changeStatus(String studentId, String targetStatus, String reason);
}
