package com.example.backend.infrastructure.security;

import com.example.backend.constant.SecurityConstants;
import com.example.backend.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        // 检查是否是SSE端点，这些端点在控制器中单独验证token
        String requestURI = request.getRequestURI();
        if (isSSEEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取请求头中的JWT令牌
        String header = request.getHeader(SecurityConstants.JWT_HEADER);
        
        // 如果请求头中没有JWT令牌，则直接放行
        if (header == null || !header.startsWith(SecurityConstants.JWT_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 从请求头中提取JWT令牌
        String token = header.substring(SecurityConstants.JWT_PREFIX.length());
        
        try {
            // 验证JWT令牌并获取用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            String userType = jwtUtils.getUserTypeFromToken(token);
            
            // 如果令牌中的用户ID有效且当前用户未认证
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 创建用户权限
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase())
                );
                
                // 创建认证令牌，使用userId作为principal
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, authorities);
                
                // 设置认证详情
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 将认证信息设置到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // 如果JWT令牌验证失败，清除安全上下文
            SecurityContextHolder.clearContext();
        }
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 检查是否是SSE端点
     * 这些端点通过URL参数传递token，在控制器中单独验证
     */
    private boolean isSSEEndpoint(String requestURI) {
        return requestURI.startsWith("/pricing/stream") ||
               requestURI.startsWith("/charge/status/stream") ||
               requestURI.startsWith("/queue/status/stream") ||
               requestURI.startsWith("/notifications/");
    }
} 