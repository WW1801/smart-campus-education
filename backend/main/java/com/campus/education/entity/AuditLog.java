package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private String userId;
    private String username;
    private String operation;
    private String module;
    private String targetType;
    private String targetId;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
