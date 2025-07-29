package com.example.backend.controller;

import com.example.backend.model.dto.PileFailureNotificationDTO;
import com.example.backend.model.entity.User;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通知控制器
 * 提供SSE端点用于向用户推送各种通知消息
 * 遵循单一职责原则：只负责处理HTTP请求和响应
 * 遵循依赖倒置原则：依赖抽象接口而非具体实现
 * 
 * @author System
 * @since 1.0
 */
@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
public class NotificationController extends BaseController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 连接用户通知SSE流
     * 用于接收各种类型的通知消息，如充电桩故障通知等
     * 
     * @param token JWT令牌
     * @return SSE发射器
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectNotifications(@RequestParam("token") String token) {
        // 验证token并获取用户ID
        Long userId = validateAndSetSecurityContext(token);
        if (userId == null) {
            throw new RuntimeException("无效的令牌或无法获取用户信息");
        }
        
        // 验证用户存在
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 创建通知连接的key
        String notificationKey = "notification_user_" + userId;
        
        // 创建SSE连接，初始状态为空通知
        SseEmitter emitter = createSseEmitter(notificationKey, () -> {
            // 连接建立时发送一个确认消息
            return new PileFailureNotificationDTO(
                "SYSTEM", 
                null, 
                null,
                "通知连接已建立",
                "您将在此接收重要通知消息",
                "INFO"
            );
        });
        
        return emitter;
    }
} 