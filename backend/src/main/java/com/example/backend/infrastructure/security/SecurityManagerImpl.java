package com.example.backend.infrastructure.security;

import com.example.backend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

/**
 * 安全管理器实现类
 * 遵循单一职责原则：专门处理安全相关的操作
 */
@Component
public class SecurityManagerImpl implements SecurityManager {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public Long getCurrentUserId() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null || context.getAuthentication() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未授权的访问");
        }
        
        Object principal = context.getAuthentication().getPrincipal();
    
        if (principal instanceof UserDetails) {
            try {
                return Long.parseLong(((UserDetails) principal).getUsername());
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户ID格式错误");
            }
        } else if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户ID格式错误");
            }
        }
        
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的用户认证信息");
    }

    @Override
    public Long validateTokenAndSetContext(String token) {
        if (token == null || token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未提供令牌");
        }
        
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String userType = jwtUtils.getUserTypeFromToken(token);
            
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的令牌");
            }
            
            // 手动设置认证信息到SecurityContext
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase())
            );
            
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId.toString(), null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            return userId;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "令牌验证失败");
        }
    }

    @Override
    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    @Override
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }
} 