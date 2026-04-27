package com.campus.education.service.impl;

/**
 * 角色服务实现类，负责处理角色相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Role;
import com.campus.education.mapper.RoleMapper;
import com.campus.education.service.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
