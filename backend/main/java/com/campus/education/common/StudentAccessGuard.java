package com.campus.education.common;

/**
 * 学生访问校验类，负责限制学生角色的数据访问范围。
 */

import com.campus.education.entity.User;
import com.campus.education.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StudentAccessGuard {

    @Autowired
    private UserService userService;

    // 处理当前学生编号
    public String currentStudentId(Authentication authentication, String requestedStudentId) {
        User user = currentUser(authentication);
        if (!"5".equals(user.getRoleId())) {
            throw new BusinessException(403, "student role required");
        }
        return resolveStudentId(user, requestedStudentId);
    }

    // 解析学生过滤
    public String resolveStudentFilter(Authentication authentication, String requestedStudentId) {
        User user = currentUser(authentication);
        if ("5".equals(user.getRoleId())) {
            return resolveStudentId(user, requestedStudentId);
        }
        return requestedStudentId;
    }

    // 解析学生编号
    private String resolveStudentId(User user, String requestedStudentId) {
        String relatedId = user.getRelatedId();
        if (relatedId == null || relatedId.trim().isEmpty()) {
            throw new BusinessException(403, "student account is not bound to a student profile");
        }
        if (requestedStudentId != null && !requestedStudentId.trim().isEmpty()
                && !relatedId.equals(requestedStudentId)) {
            throw new BusinessException(403, "cannot access another student's data");
        }
        return relatedId;
    }

    // 校验学生访问
    public void verifyStudentAccess(Authentication authentication, String studentId) {
        User user = currentUser(authentication);
        if ("5".equals(user.getRoleId())) {
            String relatedId = user.getRelatedId();
            if (relatedId == null || relatedId.trim().isEmpty() || !relatedId.equals(studentId)) {
                throw new BusinessException(403, "cannot access another student's data");
            }
        }
    }

    // 返回当前用户
    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(401, "authentication required");
        }
        User user = userService.getById(authentication.getName());
        if (user == null) {
            throw new BusinessException(401, "authentication user not found");
        }
        return user;
    }
}
