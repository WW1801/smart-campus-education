package com.campus.education.service;

/**
 * 考勤服务接口，定义考勤相关业务能力。
 */

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.education.dto.attendance.BatchAttendanceRequest;
import com.campus.education.entity.Attendance;

import java.util.List;
import java.util.Map;

public interface AttendanceService extends IService<Attendance> {

    /**
     * 学业预警的固定考勤数据源：按学生及可选学期返回考勤记录。
     * Agent 仅消费结果，不接收或拼接 SQL。
     */
    List<Attendance> listByStudentForWarning(String studentId, String semesterId);

    Map<String, Object> batchSave(BatchAttendanceRequest request, String teacherId);

    void saveManual(Attendance attendance);

    void updateManual(Attendance attendance);

    void removeManual(String attendanceId);
}
