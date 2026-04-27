package com.campus.education.entity;

/**
 * 课程实体类，负责映射课程相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.ASSIGN_ID)
    private String courseId;
    private String name;
    private String code;
    private Double credits;
    private Integer hours;
    private String type;
    private String departmentId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
