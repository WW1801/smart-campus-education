package com.campus.education.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 单个日期或字段的请假同步失败明细。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveSyncFailure {
    private String requestId;
    private LocalDate date;
    private String field;
    private String reason;
    private String suggestion;
}
