package com.example.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类，用于生成和验证JWT令牌
 */
@Component
public class JwtUtils {

    // JWT密钥
    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // JWT过期时间，设置为24小时
    private final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24小时

    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(getClaimFromToken(token, Claims::getSubject));
    }

    /**
     * 从令牌中获取用户类型
     */
    public String getUserTypeFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 从令牌中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从令牌中获取声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 获取令牌中的所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查令牌是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 为用户生成令牌
     */
    public String generateToken(String userType, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", userType);
        return doGenerateToken(claims, userId.toString());
    }

    /**
     * 生成令牌的核心方法
     */
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 验证令牌
     */
    public Boolean validateToken(String token, Long userId) {
        final Long tokenUserId = getUserIdFromToken(token);
        return (tokenUserId.equals(userId) && !isTokenExpired(token));
    }
} 