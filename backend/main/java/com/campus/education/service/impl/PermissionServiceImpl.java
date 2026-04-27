package com.campus.education.service.impl;

/**
 * 权限服务实现类，负责处理权限相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Permission;
import com.campus.education.mapper.PermissionMapper;
import com.campus.education.service.PermissionService;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
}
