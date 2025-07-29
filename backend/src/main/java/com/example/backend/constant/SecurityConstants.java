package com.example.backend.constant;

/**
 * 安全相关常量
 */
public class SecurityConstants {

    /**
     * JWT令牌请求头名称
     */
    public static final String JWT_HEADER = "Authorization";
    
    /**
     * JWT令牌前缀
     */
    public static final String JWT_PREFIX = "Bearer ";
    
    /**
     * 登录接口路径
     */
    public static final String LOGIN_PATH = "/login";
    
    /**
     * 注册接口路径
     */
    public static final String REGISTER_PATH = "/register";
    
    /**
     * 管理员类型
     */
    public static final String ADMIN_TYPE = "admin";
    
    /**
     * 普通用户类型
     */
    public static final String USER_TYPE = "user";
} 