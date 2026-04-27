package com.campus.education.entity;

/**
 * 角色权限关联实体类，负责映射角色权限关联相关业务数据。
 */

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
