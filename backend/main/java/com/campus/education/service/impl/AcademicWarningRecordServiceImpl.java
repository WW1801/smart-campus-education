package com.campus.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.dto.agent.AcademicWarningDetailDTO;
import com.campus.education.dto.agent.AcademicWarningProcessUpdateDTO;
import com.campus.education.dto.agent.AcademicWarningRecordDTO;
import com.campus.education.entity.AcademicWarningRecord;
import com.campus.education.entity.Student;
import com.campus.education.mapper.AcademicWarningRecordMapper;
import com.campus.education.mapper.StudentMapper;
import com.campus.education.service.AcademicWarningRecordService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AcademicWarningRecordServiceImpl
        extends ServiceImpl<AcademicWarningRecordMapper, AcademicWarningRecord>
        implements AcademicWarningRecordService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AcademicWarningRecordMapper academicWarningRecordMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Override
    @Transactional
    public AcademicWarningRecordDTO createFromDetail(AcademicWarningDetailDTO detail) {
        if (detail == null || detail.getStudentId() == null) {
            throw new BusinessException("预警详情不能为空");
        }
        if (detail.getTriggeredRules() == null || detail.getTriggeredRules().isEmpty()) {
            throw new BusinessException("当前学生未触发学业预警");
        }

        LocalDateTime now = LocalDateTime.now();
        AcademicWarningRecord record = new AcademicWarningRecord();
        record.setStudentId(detail.getStudentId());
        record.setSemesterId(detail.getSemesterId());
        record.setRiskLevel(detail.getRiskLevel());
        record.setRiskScore(detail.getRiskScore());
        record.setRiskReason(detail.getRiskReason());
        record.setTriggeredRules(writeRules(detail.getTriggeredRules()));
        record.setCalculatedAt(now);
        record.setProcessStatus(STATUS_PENDING);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        academicWarningRecordMapper.insert(record);
        return toDTO(record, detail.getStudentName());
    }

    @Override
    public IPage<AcademicWarningRecordDTO> pageStudentHistory(String studentId, String semesterId, String processStatus,
                                                              String riskLevel, Integer page, Integer pageSize) {
        // studentId 使用 student 表的 String 主键，先校验学生存在，避免无效外键查询被误判为空历史。
        String normalizedStudentId = normalize(studentId);
        if (normalizedStudentId == null) {
            throw new BusinessException("学生ID不能为空");
        }
        Student student = studentMapper.selectById(normalizedStudentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }

        long current = page == null || page < 1 ? 1L : page;
        long size = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100);
        Page<AcademicWarningRecord> sourcePage = new Page<>(current, size);
        LambdaQueryWrapper<AcademicWarningRecord> wrapper = new LambdaQueryWrapper<AcademicWarningRecord>()
                .eq(AcademicWarningRecord::getStudentId, normalizedStudentId)
                .eq(normalize(semesterId) != null, AcademicWarningRecord::getSemesterId, normalize(semesterId))
                .eq(normalize(processStatus) != null, AcademicWarningRecord::getProcessStatus, normalize(processStatus))
                .eq(normalize(riskLevel) != null, AcademicWarningRecord::getRiskLevel, normalize(riskLevel))
                .orderByDesc(AcademicWarningRecord::getCalculatedAt);

        // 复用 MyBatis Plus 条件分页，按计算时间倒序查询；不拼接 SQL。
        IPage<AcademicWarningRecord> source = academicWarningRecordMapper.selectPage(sourcePage, wrapper);
        Page<AcademicWarningRecordDTO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        // 规则 JSON 解析失败或字段为空时回退为空数组，保证前端 records 和 triggeredRules 可直接消费。
        result.setRecords(source.convert(record -> toDTO(record, student.getName())).getRecords());
        return result;
    }

    @Override
    @Transactional
    public AcademicWarningRecordDTO updateProcess(String recordId, AcademicWarningProcessUpdateDTO request,
                                                  String processedBy) {
        String normalizedRecordId = normalize(recordId);
        if (normalizedRecordId == null) {
            throw new BusinessException("预警记录ID不能为空");
        }
        AcademicWarningRecord record = academicWarningRecordMapper.selectById(normalizedRecordId);
        if (record == null) {
            throw new BusinessException("预警记录不存在");
        }
        String processStatus = request == null ? null : normalize(request.getProcessStatus());
        if (!isValidProcessStatus(processStatus)) {
            throw new BusinessException("处理状态仅支持 pending、processing、completed");
        }
        if (STATUS_COMPLETED.equals(processStatus)
                && (request.getProcessOpinion() == null || request.getProcessOpinion().trim().isEmpty())) {
            throw new BusinessException("已完成状态必须填写处理意见");
        }

        record.setProcessStatus(processStatus);
        record.setProcessOpinion(request.getProcessOpinion() == null ? null : request.getProcessOpinion().trim());
        record.setProcessedBy(normalize(processedBy));
        record.setProcessedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        academicWarningRecordMapper.updateById(record);

        Student student = studentMapper.selectById(record.getStudentId());
        return toDTO(record, student == null ? null : student.getName());
    }

    private AcademicWarningRecordDTO toDTO(AcademicWarningRecord record, String studentName) {
        // 实体中的规则 JSON 转换为 DTO 数组，隔离数据库存储格式。
        return AcademicWarningRecordDTO.builder()
                .recordId(record.getRecordId())
                .studentId(record.getStudentId())
                .studentName(studentName)
                .semesterId(record.getSemesterId())
                .riskLevel(record.getRiskLevel())
                .riskScore(record.getRiskScore())
                .riskReason(record.getRiskReason())
                .triggeredRules(readRules(record.getTriggeredRules()))
                .calculatedAt(record.getCalculatedAt())
                .processStatus(record.getProcessStatus())
                .processOpinion(record.getProcessOpinion())
                .processedBy(record.getProcessedBy())
                .processedAt(record.getProcessedAt())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private String writeRules(List<String> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception exception) {
            throw new BusinessException("预警规则序列化失败");
        }
    }

    private List<String> readRules(String rules) {
        if (rules == null || rules.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rules, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private boolean isValidProcessStatus(String status) {
        return STATUS_PENDING.equals(status) || STATUS_PROCESSING.equals(status) || STATUS_COMPLETED.equals(status);
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
