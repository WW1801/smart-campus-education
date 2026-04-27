package com.campus.education.controller.course;

/**
 * 教学计划控制器，负责处理教学计划相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.BusinessIdGenerator;
import com.campus.education.common.Result;
import com.campus.education.entity.TeachingPlan;
import com.campus.education.service.TeachingPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teaching-plan")
public class TeachingPlanController {

    @Autowired
    private TeachingPlanService teachingPlanService;

    @Autowired
    private BusinessIdGenerator businessIdGenerator;

    // 分页查询教学计划
    @GetMapping("/page")
    public Result<IPage<TeachingPlan>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String majorId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String courseNature) {
        Page<TeachingPlan> page = new Page<>(current, size);
        LambdaQueryWrapper<TeachingPlan> wrapper = new LambdaQueryWrapper<>();
        if (majorId != null && !majorId.trim().isEmpty()) {
            wrapper.eq(TeachingPlan::getMajorId, majorId);
        }
        if (courseId != null && !courseId.trim().isEmpty()) {
            wrapper.eq(TeachingPlan::getCourseId, courseId);
        }
        if (courseNature != null && !courseNature.trim().isEmpty()) {
            wrapper.eq(TeachingPlan::getCourseNature, courseNature);
        }
        wrapper.orderByAsc(TeachingPlan::getMajorId, TeachingPlan::getSemesterType);
        return Result.success(teachingPlanService.page(page, wrapper));
    }

    // 查询教学计划列表
    @GetMapping("/list")
    public Result<List<TeachingPlan>> list(
            @RequestParam(required = false) String majorId) {
        LambdaQueryWrapper<TeachingPlan> wrapper = new LambdaQueryWrapper<>();
        if (majorId != null && !majorId.trim().isEmpty()) {
            wrapper.eq(TeachingPlan::getMajorId, majorId);
        }
        wrapper.orderByAsc(TeachingPlan::getSemesterType);
        return Result.success(teachingPlanService.list(wrapper));
    }

    // 获取教学计划详情
    @GetMapping("/{id}")
    public Result<TeachingPlan> getById(@PathVariable String id) {
        return Result.success(teachingPlanService.getById(id));
    }

    // 添加教学计划
    @PostMapping
    public Result<Void> add(@RequestBody TeachingPlan plan) {
        LambdaQueryWrapper<TeachingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingPlan::getMajorId, plan.getMajorId());
        wrapper.eq(TeachingPlan::getCourseId, plan.getCourseId());
        if (teachingPlanService.count(wrapper) > 0) {
            return Result.badRequest("该专业已存在此课程的教学计划");
        }
        if (plan.getPlanId() == null || plan.getPlanId().trim().isEmpty()) {
            plan.setPlanId(businessIdGenerator.nextPrefixedId("teaching_plan", "plan_id", "TP", 3));
        }
        teachingPlanService.save(plan);
        return Result.success("添加成功", null);
    }

    // 更新教学计划
    @PutMapping
    public Result<Void> update(@RequestBody TeachingPlan plan) {
        teachingPlanService.updateById(plan);
        return Result.success("更新成功", null);
    }

    // 删除教学计划
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        teachingPlanService.removeById(id);
        return Result.success("删除成功", null);
    }
}
