package com.campus.education.entity;

/**
 * 班级实体类，负责映射班级相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("class")
public class Class {
    @TableId(type = IdType.ASSIGN_ID)
    private String classId;
    private String name;
    private String majorId;
    private String grade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
