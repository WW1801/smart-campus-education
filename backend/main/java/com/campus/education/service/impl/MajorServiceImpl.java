package com.campus.education.service.impl;

/**
 * 专业服务实现类，负责处理专业相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Major;
import com.campus.education.mapper.MajorMapper;
import com.campus.education.service.MajorService;
import org.springframework.stereotype.Service;

@Service
public class MajorServiceImpl extends ServiceImpl<MajorMapper, Major> implements MajorService {
}
