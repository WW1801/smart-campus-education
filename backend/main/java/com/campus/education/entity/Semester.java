package com.campus.education.entity;

/**
 * 学期实体类，负责映射学期相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("semester")
public class Semester {
    @TableId(type = IdType.ASSIGN_ID)
    private String semesterId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer teachingWeeks;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
