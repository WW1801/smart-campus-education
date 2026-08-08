package com.campus.education.controller.system;

import com.campus.education.common.Result;
import com.campus.education.service.AccountBatchProvisioningService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** 管理员批量补开历史人员缺失的登录账号。 */
@RestController
@RequestMapping("/system/account-provisioning")
public class AccountProvisioningController {
    private final AccountBatchProvisioningService service;
    public AccountProvisioningController(AccountBatchProvisioningService service) { this.service = service; }
    @PostMapping("/batch")
    public Result<Map<String, Object>> batch(@RequestBody Map<String, String> request) {
        return Result.success("批量开通已完成，请查看成功与失败明细。", service.provision(
                request.get("personType"), request.get("departmentId"), request.get("classId")));
    }
}
