package com.example.backend.config;

import com.example.backend.constant.SecurityConstants;
import com.example.backend.infrastructure.security.CustomUserDetailsService;
import com.example.backend.infrastructure.security.JwtAuthenticationEntryPoint;
import com.example.backend.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Web安全配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 配置认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * 配置认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护
            .csrf(csrf -> csrf.disable())
            // 配置异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
            )
            // 配置会话管理，使用无状态会话
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许所有人访问登录和注册接口
                .requestMatchers(SecurityConstants.LOGIN_PATH, SecurityConstants.REGISTER_PATH).permitAll()
                // 允许所有SSE相关端点，这些端点将在控制器中进行令牌验证
                // 注意：前端通过/api/xxx访问，经过代理后变成/xxx，所以这里的路径不需要/api前缀
                .requestMatchers("/pricing/stream", "/pricing/stream/**", 
                                "/charge/status/stream/**",
                                "/queue/status/stream/**",
                                "/notifications/connect", "/notifications/**").permitAll()
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            );
        
        // 添加认证提供者
        http.authenticationProvider(authenticationProvider());
        
        // 在UsernamePasswordAuthenticationFilter之前添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    /**
     * 配置跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 设置允许的来源
        config.addAllowedOriginPattern("*");
        // 设置允许的请求头
        config.addAllowedHeader("*");
        // 设置允许的请求方法
        config.addAllowedMethod("*");
        // 添加映射路径，拦截一切请求
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 