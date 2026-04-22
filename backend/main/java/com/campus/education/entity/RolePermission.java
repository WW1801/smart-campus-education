package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("role_permission")
public class RolePermission {
    @TableId(type = IdType.INPUT)
    private String roleId;
    private String permissionId;
    private LocalDateTime createdAt;
}
