package com.campus.education.service.impl;

/**
 * 学生服务实现类，负责处理学生相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.entity.Student;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.StudentService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = createTransitions();

    // 初始化状态流转规则
    private static Map<String, Set<String>> createTransitions() {
        Map<String, Set<String>> map = new java.util.HashMap<>();
        map.put("active", new HashSet<>(Arrays.asList("suspended", "graduated", "dropped")));
        map.put("suspended", new HashSet<>(Arrays.asList("active", "dropped")));
        return map;
    }

    // 变更状态
    @Override
    public void changeStatus(String studentId, String targetStatus, String reason) {
        Student student = this.getById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        String currentStatus = student.getStatus();
        if (currentStatus.equals(targetStatus)) {
            throw new BusinessException("当前状态与目标状态相同");
        }
        if ("graduated".equals(currentStatus)) {
            throw new BusinessException("毕业为终态，不可变更");
        }
        if ("dropped".equals(currentStatus)) {
            throw new BusinessException("退学状态不可直接恢复，需走重新注册流程");
        }
        Set<String> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new BusinessException(String.format("不允许从 %s 变更为 %s", currentStatus, targetStatus));
        }
        student.setStatus(targetStatus);
        this.updateById(student);
    }
}
