package com.campus.education.entity;

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
