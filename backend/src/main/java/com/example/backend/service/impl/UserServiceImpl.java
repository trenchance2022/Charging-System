package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.constant.SecurityConstants;
import com.example.backend.exception.BatteryCapacityValidationException;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.LoginRequest;
import com.example.backend.model.dto.LoginResponse;
import com.example.backend.model.dto.RegisterRequest;
import com.example.backend.model.dto.RegisterResponse;
import com.example.backend.model.entity.User;
import com.example.backend.service.UserService;
import com.example.backend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // 使用Spring Security的认证管理器进行认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(), 
                    loginRequest.getPassword()
                )
            );
            
            // 设置认证信息到SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 查询用户信息，获取用户类型
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", loginRequest.getUsername())
                       .eq("type", loginRequest.getType());
            
            User user = userMapper.selectOne(queryWrapper);
            
            if (user == null || !user.getType().equals(loginRequest.getType())) {
                throw new BadCredentialsException("用户类型不匹配");
            }
            
            // 生成JWT令牌
            String token = jwtUtils.generateToken(user.getType(), user.getId());
            
            // 返回登录响应
            return new LoginResponse(token, user.getType());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("用户名或密码错误");
        }
    }
    
    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        // 验证用户类型是否有效
        if (!SecurityConstants.USER_TYPE.equals(registerRequest.getType()) && 
            !SecurityConstants.ADMIN_TYPE.equals(registerRequest.getType())) {
            throw new RuntimeException("无效的用户类型");
        }
        
        // 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", registerRequest.getUsername());
        
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        // 对密码进行加密
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setType(registerRequest.getType());
        
        // 保存用户到数据库
        userMapper.insert(user);
        
        // 返回注册成功的响应
        return new RegisterResponse("注册成功");
    }
    
    @Override
    public User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }
    
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
    
    @Override
    public boolean updateBatteryCapacity(Long userId, Double batteryCapacity) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BatteryCapacityValidationException("用户不存在");
            }
            
            // 验证电池容量不能小于当前电量
            Double currentPower = user.getCurrentPower();
            if (currentPower != null && batteryCapacity < currentPower) {
                throw new BatteryCapacityValidationException("电池容量不能小于当前电量 " + currentPower + " kWh");
            }
            
            user.setBatteryCapacity(batteryCapacity);
            int result = userMapper.updateById(user);
            return result > 0;
        } catch (BatteryCapacityValidationException e) {
            // 重新抛出业务验证异常，让全局异常处理器处理
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("更新电池容量失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean updateCurrentPower(Long userId, Double currentPower) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }
            
            user.setCurrentPower(currentPower);
            int result = userMapper.updateById(user);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }
} 