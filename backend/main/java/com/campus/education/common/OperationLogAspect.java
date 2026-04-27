package com.campus.education.common;

/**
 * 操作日志切面类，负责记录接口调用过程中的关键日志。
 */

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    // 定义控制器切点
    @Pointcut("execution(* com.campus.education.controller..*.*(..))")
    public void controllerPointcut() {}

    // 执行环绕通知
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = "";
        String uri = "";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = request.getRemoteAddr();
            uri = request.getRequestURI();
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.error("接口异常: uri={}, method={}, ip={}, error={}", uri, methodName, ip, e.getMessage());
            throw e;
        }

        long costTime = System.currentTimeMillis() - startTime;
        if (costTime > 2000) {
            log.warn("慢接口: uri={}, method={}, ip={}, cost={}ms", uri, methodName, ip, costTime);
        } else {
            log.info("接口调用: uri={}, method={}, ip={}, cost={}ms", uri, methodName, ip, costTime);
        }

        return result;
    }
}
