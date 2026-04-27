package com.campus.education.mapper;

/**
 * 角色权限关联数据访问接口，负责执行角色权限关联相关持久化操作。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.education.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
