package com.campus.education.controller.system;

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

    @GetMapping("/list")
    public Result<List<Semester>> list() {
        return Result.success(semesterService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Semester semester) {
        semesterService.save(semester);
        return Result.success("添加成功", null);
    }

    @PutMapping
    public Result<Void> update(@RequestBody Semester semester) {
        semesterService.updateById(semester);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        semesterService.removeById(id);
        return Result.success("删除成功", null);
    }
}
