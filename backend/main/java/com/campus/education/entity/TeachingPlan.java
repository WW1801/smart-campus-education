package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teaching_plan")
public class TeachingPlan {
    @TableId(type = IdType.ASSIGN_ID)
    private String planId;
    private String majorId;
    private String courseId;
    private Integer semesterType;
    private String courseNature;
    private Integer isPrerequisite;
    private String prerequisiteIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
