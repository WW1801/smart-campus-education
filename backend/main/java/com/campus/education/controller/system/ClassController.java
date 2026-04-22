package com.campus.education.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.common.Result;
import com.campus.education.entity.Class;
import com.campus.education.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class")
public class ClassController {

    @Autowired
    private ClassService classService;

    @GetMapping("/list")
    public Result<List<Class>> list(@RequestParam(required = false) String majorId) {
        LambdaQueryWrapper<Class> wrapper = new LambdaQueryWrapper<>();
        if (majorId != null && !majorId.trim().isEmpty()) {
            wrapper.eq(Class::getMajorId, majorId);
        }
        return Result.success(classService.list(wrapper));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Class clazz) {
        classService.save(clazz);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Class clazz) {
        classService.updateById(clazz);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        classService.removeById(id);
        return Result.success("删除成功", null);
    }
}
