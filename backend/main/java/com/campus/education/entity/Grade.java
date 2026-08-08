package com.campus.education.entity;

/**
 * 成绩实体类，负责映射成绩相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("grade")
public class Grade {
    @TableId(type = IdType.ASSIGN_ID)
    private String gradeId;
    private String studentId;
    @TableField(exist = false)
    private String studentName;
    private String courseId;
    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private Double credits;
    @TableField(exist = false)
    private Double gradePoint;
    private String semesterId;
    private String teacherId;
    private Double usualScore;
    private Double examScore;
    private Double totalScore;
    private String status;
    private Boolean isPass;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
