package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("teacher")
public class Teacher {
    @TableId(type = IdType.ASSIGN_ID)
    private String teacherId;
    private String name;
    private String gender;
    private LocalDate birthdate;
    private String phone;
    private String email;
    private String departmentId;
    private String title;
    private String specialty;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
