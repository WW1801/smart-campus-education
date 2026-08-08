package com.campus.education.service;

/**
 * 学生服务接口，定义学生相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.entity.Student;

public interface StudentService extends IService<Student> {
    /** 新增学生并生成唯一业务学号，studentId 仍作为内部主键。 */
    void createStudent(Student student);

    /** 更新学生基础资料时保留已生成的业务学号和内部主键。 */
    void updateStudent(Student student);

    // 变更状态
    void changeStatus(String studentId, String targetStatus, String reason);
}
