package com.campus.education.entity;

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
