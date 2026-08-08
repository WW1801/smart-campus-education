package com.campus.education.entity;

/**
 * 学生实体类，负责映射学生相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {
    @TableId(type = IdType.ASSIGN_ID)
    private String studentId;
    /** 对外展示的业务学号：入学年份 + 院系代码 + 专业代码 + 专业内三位序号。 */
    private String studentNo;
    private String name;
    private String gender;
    private LocalDate birthdate;
    private String phone;
    private String email;
    private String departmentId;
    private String majorId;
    private String classId;
    private LocalDate enrollmentDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
