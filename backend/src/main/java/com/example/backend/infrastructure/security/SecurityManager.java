package com.example.backend.infrastructure.security;

import org.springframework.web.server.ResponseStatusException;

/**
 * 安全管理器接口
 * 遵循单一职责原则：专门处理安全相关的操作
 * 遵循依赖倒置原则：依赖抽象而非具体实现
 */
public interface SecurityManager {
    
    /**
     * 从安全上下文中获取用户ID
     * @return 用户ID
     * @throws ResponseStatusException 如果未授权
     */
    Long getCurrentUserId();
    
    /**
     * 验证JWT令牌并设置安全上下文
     * @param token JWT令牌
     * @return 用户ID
     * @throws ResponseStatusException 如果验证失败
     */
    Long validateTokenAndSetContext(String token);
    
    /**
     * 检查当前用户是否有指定权限
     * @param authority 权限名称
     * @return 是否有权限
     */
    boolean hasAuthority(String authority);
    
    /**
     * 清空安全上下文
     */
    void clearContext();
} 