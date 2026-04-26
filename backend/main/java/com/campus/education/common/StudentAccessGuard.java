package com.campus.education.common;

import com.campus.education.entity.User;
import com.campus.education.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StudentAccessGuard {

    @Autowired
    private UserService userService;

    public String currentStudentId(Authentication authentication, String requestedStudentId) {
        User user = currentUser(authentication);
        if (!"5".equals(user.getRoleId())) {
            throw new BusinessException(403, "student role required");
        }
        return resolveStudentId(user, requestedStudentId);
    }

    public String resolveStudentFilter(Authentication authentication, String requestedStudentId) {
        User user = currentUser(authentication);
        if ("5".equals(user.getRoleId())) {
            return resolveStudentId(user, requestedStudentId);
        }
        return requestedStudentId;
    }

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

    public void verifyStudentAccess(Authentication authentication, String studentId) {
        User user = currentUser(authentication);
        if ("5".equals(user.getRoleId())) {
            String relatedId = user.getRelatedId();
            if (relatedId == null || relatedId.trim().isEmpty() || !relatedId.equals(studentId)) {
                throw new BusinessException(403, "cannot access another student's data");
            }
        }
    }

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
