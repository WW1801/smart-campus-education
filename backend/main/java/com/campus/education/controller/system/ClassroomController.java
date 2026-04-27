package com.campus.education.controller.system;

/**
 * 教室控制器，负责处理教室相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.common.Result;
import com.campus.education.entity.Classroom;
import com.campus.education.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/classroom")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @GetMapping("/list")
    public Result<List<Classroom>> list(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Classroom::getStatus, status);
        }
        wrapper.orderByAsc(Classroom::getBuilding, Classroom::getName);
        return Result.success(classroomService.list(wrapper));
    }
}
