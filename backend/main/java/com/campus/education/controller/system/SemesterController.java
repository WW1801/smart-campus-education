package com.campus.education.controller.system;

/**
 * 学期控制器，负责处理学期相关接口请求。
 */

import com.campus.education.common.Result;
import com.campus.education.entity.Semester;
import com.campus.education.service.SemesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/semester")
public class SemesterController {

    @Autowired
    private SemesterService semesterService;

    // 查询学期列表
    @GetMapping("/list")
    public Result<List<Semester>> list() {
        return Result.success(semesterService.list());
    }

    // 添加学期
    @PostMapping
    public Result<Void> add(@RequestBody Semester semester) {
        semesterService.save(semester);
        return Result.success("添加成功", null);
    }

    // 更新学期
    @PutMapping
    public Result<Void> update(@RequestBody Semester semester) {
        semesterService.updateById(semester);
        return Result.success("更新成功", null);
    }

    // 删除学期
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        semesterService.removeById(id);
        return Result.success("删除成功", null);
    }
}
