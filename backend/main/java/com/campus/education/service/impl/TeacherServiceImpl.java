package com.campus.education.service.impl;

/**
 * 教师服务实现类，负责处理教师相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Teacher;
import com.campus.education.common.BusinessException;
import com.campus.education.mapper.TeacherMapper;
import com.campus.education.service.TeacherService;
import org.springframework.stereotype.Service;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {
    @Override
    public void changeStatus(String teacherId, String targetStatus) {
        if (!"active".equals(targetStatus) && !"inactive".equals(targetStatus) && !"resigned".equals(targetStatus)) {
            throw new BusinessException("教师状态仅支持 active（在职）、inactive（停用）或 resigned（离职）。");
        }
        Teacher teacher = getById(teacherId);
        if (teacher == null) throw new BusinessException("教师不存在，请刷新列表后重试。");
        if (targetStatus.equals(teacher.getStatus())) throw new BusinessException("教师当前已是目标状态，无需重复操作。");
        teacher.setStatus(targetStatus);
        updateById(teacher);
    }
}
