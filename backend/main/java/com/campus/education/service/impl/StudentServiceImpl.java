package com.campus.education.service.impl;

/**
 * 学生服务实现类，负责处理学生相关业务逻辑。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.entity.Student;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.StudentService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = createTransitions();

    @Override
    public void createStudent(Student student) {
        validateStudentScope(student);
        // 业务学号由稳定业务编号生成，数据库 studentId 仍由 ASSIGN_ID 作为内部关联主键。
        student.setStudentNo(generateStudentNo(student));
        student.setStatus("active");
        this.save(student);
    }

    @Override
    public void updateStudent(Student student) {
        Student existing = this.getById(student.getStudentId());
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }
        // 学号生成后不可随院系、专业或班级编辑而改变，确保对外凭证稳定。
        student.setStudentNo(existing.getStudentNo());
        this.updateById(student);
    }

    /**
     * 生成规则：入学年份(4位) + 院系编号末3位 + 专业编号末3位 + 专业内递增序号(3位)。
     * 例如 2026 + D001 + M001 + 001 生成 2026001001001。
     */
    private String generateStudentNo(Student student) {
        int enrollmentYear = student.getEnrollmentDate() == null
                ? LocalDate.now().getYear() : student.getEnrollmentDate().getYear();
        String prefix = enrollmentYear + businessCode(student.getDepartmentId())
                + businessCode(student.getMajorId());
        int maxSequence = 0;
        Set<String> existingNumbers = new HashSet<>();
        for (Student item : this.list(new LambdaQueryWrapper<Student>()
                .likeRight(Student::getStudentNo, prefix))) {
            String studentNo = item.getStudentNo();
            if (studentNo != null && studentNo.length() == prefix.length() + 3) {
                try {
                    if (!existingNumbers.add(studentNo)) {
                        throw new BusinessException("发现重复学号，请先执行学号迁移校验并处理重复数据后再新增学生");
                    }
                    maxSequence = Math.max(maxSequence, Integer.parseInt(studentNo.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // 历史异常学号不参与序号计算，避免阻断新的标准学号生成。
                }
            }
        }
        if (maxSequence >= 999) {
            throw new BusinessException("该专业当年学生数量已达到学号序号上限");
        }
        return prefix + String.format("%03d", maxSequence + 1);
    }

    /** 复用当前 D001/M001 形式的业务标识，避免依赖数据库雪花主键。 */
    private String businessCode(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() == 0) {
            throw new BusinessException("院系和专业必须使用带数字的业务编号");
        }
        return String.format("%3s", digits.substring(Math.max(0, digits.length() - 3))).replace(' ', '0');
    }

    private void validateStudentScope(Student student) {
        if (student == null || student.getDepartmentId() == null || student.getMajorId() == null || student.getClassId() == null) {
            throw new BusinessException("生成学号前必须选择院系、专业和班级");
        }
    }

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
