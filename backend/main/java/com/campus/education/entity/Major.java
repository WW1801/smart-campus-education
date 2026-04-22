package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("major")
public class Major {
    @TableId(type = IdType.ASSIGN_ID)
    private String majorId;
    private String name;
    private String departmentId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
