package com.example.backend.controller;

import com.example.backend.model.dto.QueueStatusDTO;
import com.example.backend.service.QueueService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 队列控制器
 * 重构后遵循单一职责原则：只负责处理HTTP请求和响应
 * 遵循依赖倒置原则：依赖抽象接口而非具体实现
 */
@RestController
@RequestMapping("/queue")
public class QueueController extends BaseController {
    
    private final QueueService queueService;
    
    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }
    
    /**
     * 获取用户的队列状态
     */
    @GetMapping("/user")
    public ResponseEntity<QueueStatusDTO> getUserQueueStatus() {
        Long userId = getUserIdFromSecurity();
        QueueStatusDTO statusDTO = queueService.getUserQueueStatus(userId);
        return ResponseEntity.ok(statusDTO);
    }
    
    /**
     * 建立SSE连接，获取用户队列状态流
     */
    @GetMapping(value = "/status/stream/user", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamUserQueueStatus(
            @RequestParam(name = "token", required = true) String token) {
        
        Long userId = validateAndSetSecurityContext(token);
        String userKey = "queue_user_" + userId;
        
        SseEmitter emitter = createSseEmitter(userKey, 
            () -> queueService.getUserQueueStatus(userId));
        return emitter;
    }
} 