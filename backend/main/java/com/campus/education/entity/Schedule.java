package com.campus.education.entity;

/**
 * 课表实体类，负责映射课表相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 自动排课兼容实体。字段统一继承自 {@link CourseSchedule}，避免两个同表实体的字段定义继续分叉。
 */
@TableName("course_schedule")
public class Schedule extends CourseSchedule {
}
