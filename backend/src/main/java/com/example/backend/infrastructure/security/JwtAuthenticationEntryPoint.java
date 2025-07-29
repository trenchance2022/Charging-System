package com.example.backend.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证入口点，处理认证失败的情况
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 设置响应状态码为401（未授权）
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 设置响应内容类型为JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 构建错误响应内容
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("message", "未授权：" + authException.getMessage());
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        
        // 将错误响应写入响应输出流
        new ObjectMapper().writeValue(response.getOutputStream(), errorDetails);
    }
} 