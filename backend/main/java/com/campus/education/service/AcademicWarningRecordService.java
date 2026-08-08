package com.campus.education.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.dto.agent.AcademicWarningDetailDTO;
import com.campus.education.dto.agent.AcademicWarningProcessUpdateDTO;
import com.campus.education.dto.agent.AcademicWarningRecordDTO;
import com.campus.education.entity.AcademicWarningRecord;

public interface AcademicWarningRecordService extends IService<AcademicWarningRecord> {
    AcademicWarningRecordDTO createFromDetail(AcademicWarningDetailDTO detail);

    IPage<AcademicWarningRecordDTO> pageStudentHistory(String studentId, String semesterId, String processStatus,
                                                       String riskLevel, Integer page, Integer pageSize);

    AcademicWarningRecordDTO updateProcess(String recordId, AcademicWarningProcessUpdateDTO request, String processedBy);
}
