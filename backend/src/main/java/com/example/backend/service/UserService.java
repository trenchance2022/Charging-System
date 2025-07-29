package com.example.backend.service;

import com.example.backend.model.dto.LoginRequest;
import com.example.backend.model.dto.LoginResponse;
import com.example.backend.model.dto.RegisterRequest;
import com.example.backend.model.dto.RegisterResponse;
import com.example.backend.model.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应，包含JWT令牌和用户类型
     */
    LoginResponse login(LoginRequest loginRequest);
    
    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册响应
     */
    RegisterResponse register(RegisterRequest registerRequest);
    
    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    User getUserByUsername(String username);
    
    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    User getUserById(Long userId);
    
    /**
     * 更新用户电池容量
     * @param userId 用户ID
     * @param batteryCapacity 电池容量
     * @return 更新是否成功
     */
    boolean updateBatteryCapacity(Long userId, Double batteryCapacity);
    
    /**
     * 更新用户当前电量
     * @param userId 用户ID
     * @param currentPower 当前电量
     * @return 更新是否成功
     */
    boolean updateCurrentPower(Long userId, Double currentPower);
} 