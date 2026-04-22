package com.campus.education.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Semester;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.service.SemesterService;
import org.springframework.stereotype.Service;

@Service
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, Semester> implements SemesterService {
}
