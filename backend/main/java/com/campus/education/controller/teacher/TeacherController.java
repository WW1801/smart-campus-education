package com.campus.education.controller.teacher;

/**
 * 教师控制器，负责处理教师相关接口请求。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.education.common.Result;
import com.campus.education.entity.Teacher;
import com.campus.education.service.AccountProvisioningService;
import com.campus.education.service.TeacherService;
import com.campus.education.service.PersonnelLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AccountProvisioningService accountProvisioningService;

    @Autowired
    private PersonnelLifecycleService personnelLifecycleService;

    @GetMapping("/list")
    public Result<List<Teacher>> list(@RequestParam(required = false) String departmentId) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Teacher::getDepartmentId, departmentId);
        }
        return Result.success(teacherService.list(wrapper));
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String teacherId,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String departmentId) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null && !teacherId.trim().isEmpty()) {
            wrapper.eq(Teacher::getTeacherId, teacherId);
        }
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Teacher::getName, name);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            wrapper.eq(Teacher::getDepartmentId, departmentId);
        }
        Page<Teacher> pageParam = new Page<>(current, size);
        IPage<Teacher> result = teacherService.page(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success("查询成功", data);
    }

    // 添加教师
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> add(@RequestBody Teacher teacher) {
        if (!teacherService.save(teacher)) {
            return Result.error("新增教师失败，请稍后重试");
        }
        accountProvisioningService.provisionTeacherAccount(teacher);
        return Result.success("新增成功，已自动开通账号；初始密码为123456，请首次登录后及时修改", null);
    }

    // 更新教师
    @PutMapping
    public Result<Void> update(@RequestBody Teacher teacher) {
        teacherService.updateById(teacher);
        return Result.success("更新成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable String id, @RequestBody Map<String, String> params) {
        String status = params.get("status");
        if (status == null || status.trim().isEmpty()) return Result.badRequest("目标状态不能为空，请选择在职、停用或离职。");
        teacherService.changeStatus(id, status);
        return Result.success("教师状态已更新；停用或离职后将不能登录和办理业务。", null);
    }

    // 删除教师
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        personnelLifecycleService.verifyTeacherCanDelete(id);
        teacherService.removeById(id);
        return Result.success("删除成功", null);
    }
}
