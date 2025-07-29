package com.example.backend.controller;

import com.example.backend.infrastructure.security.SecurityManager;
import com.example.backend.infrastructure.sse.SseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Supplier;

/**
 * 基础控制器，提供公共方法
 * 遵循单一职责原则：只负责提供控制器的基础功能
 * 遵循依赖倒置原则：依赖抽象接口而非具体实现
 */
public abstract class BaseController {
    
    @Autowired
    protected SecurityManager securityManager;
    
    @Autowired
    protected SseManager sseManager;
    
    /**
     * 从安全上下文中获取用户ID
     * @return 用户ID
     */
    protected Long getUserIdFromSecurity() {
        return securityManager.getCurrentUserId();
    }
    
    /**
     * 验证JWT令牌并设置安全上下文
     * @param token JWT令牌
     * @return 用户ID
     */
    protected Long validateAndSetSecurityContext(String token) {
        return securityManager.validateTokenAndSetContext(token);
    }
    
    /**
     * 创建SSE发射器的通用方法
     * @param key 发射器标识
     * @param statusSupplier 状态供应商
     * @param <T> 状态类型
     * @return SSE发射器
     */
    protected <T> SseEmitter createSseEmitter(String key, Supplier<T> statusSupplier) {
        return sseManager.createEmitter(key, statusSupplier);
    }
    
    /**
     * 检查当前用户是否有指定权限
     * @param authority 权限名称
     * @return 是否有权限
     */
    protected boolean hasAuthority(String authority) {
        return securityManager.hasAuthority(authority);
    }
    
    /**
     * 统一的错误响应记录类
     */
    protected record ErrorResponse(String message) {}
} 