package com.campus.education.entity;

/**
 * 教学计划实体类，负责映射教学计划相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teaching_plan")
public class TeachingPlan {
    @TableId(type = IdType.INPUT)
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
