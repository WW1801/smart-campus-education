package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.INPUT)
    private String userId;
    private String username;
    private String password;
    private String name;
    private String roleId;
    private String relatedId;
    private LocalDateTime lastLogin;
    private String loginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
