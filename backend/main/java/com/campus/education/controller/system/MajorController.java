package com.campus.education.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.education.common.Result;
import com.campus.education.entity.Major;
import com.campus.education.service.MajorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/major")
public class MajorController {

    @Autowired
    private MajorService majorService;

    @GetMapping("/list")
    public Result<List<Major>> list(@RequestParam(required = false) String departmentId) {
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Major::getDepartmentId, departmentId);
        }
        return Result.success(majorService.list(wrapper));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Major major) {
        majorService.save(major);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Major major) {
        majorService.updateById(major);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        majorService.removeById(id);
        return Result.success("删除成功", null);
    }
}
