package com.example.backend.controller;

import com.example.backend.model.dto.ChargingRequestDTO;
import com.example.backend.model.dto.ChargingResponseDTO;
import com.example.backend.model.dto.ChargingStatusDTO;
import com.example.backend.service.ChargingRequestService;
import com.example.backend.service.UserService;
import com.example.backend.model.entity.User;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 充电控制器
 * 重构后遵循单一职责原则：只负责处理HTTP请求和响应
 * 遵循依赖倒置原则：依赖抽象接口而非具体实现
 */
@RestController
@RequestMapping("/charge")
public class ChargingController extends BaseController {
    private final ChargingRequestService chargingRequestService;
    private final UserService userService;
    
    public ChargingController(ChargingRequestService chargingRequestService, UserService userService) {
        this.chargingRequestService = chargingRequestService;
        this.userService = userService;
    }
    
    /**
     * 提交充电请求
     */
    @PostMapping("/request")
    public ResponseEntity<ChargingResponseDTO> submitChargingRequest(@RequestBody ChargingRequestDTO requestDTO) {
        Long userId = getUserIdFromSecurity();
        
        ChargingResponseDTO responseDTO = chargingRequestService.submitRequest(userId, requestDTO);
        
        // 如果操作成功，推送状态更新
        if (!"ERROR".equals(responseDTO.getStatus())) {
            pushChargingStatusUpdate(userId, "提交充电请求", responseDTO.getRequestId());
        }
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 修改充电请求
     */
    @PostMapping("/modify")
    public ResponseEntity<ChargingResponseDTO> modifyChargingRequest(
            @RequestBody ChargingRequestDTO requestDTO) {
        
        Long userId = getUserIdFromSecurity();
        ChargingResponseDTO responseDTO = chargingRequestService.modifyActiveRequest(userId, requestDTO);
        
        // 如果操作成功，推送状态更新
        if (!"ERROR".equals(responseDTO.getStatus())) {
            pushChargingStatusUpdate(userId, "修改充电请求", responseDTO.getRequestId());
        }
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 开始充电
     */
    @PostMapping("/start")
    public ResponseEntity<ChargingResponseDTO> startCharging() {
        Long userId = getUserIdFromSecurity();
        ChargingResponseDTO responseDTO = chargingRequestService.startCharging(userId);
        
        // 如果操作成功，推送状态更新
        if (!"ERROR".equals(responseDTO.getStatus())) {
            pushChargingStatusUpdate(userId, "开始充电", responseDTO.getRequestId());
        }
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 结束充电
     */
    @PostMapping("/stop")
    public ResponseEntity<ChargingResponseDTO> stopCharging() {
        Long userId = getUserIdFromSecurity();
        ChargingResponseDTO responseDTO = chargingRequestService.stopCharging(userId);
        
        // 如果操作成功，推送状态更新
        if (!"ERROR".equals(responseDTO.getStatus())) {
            pushChargingStatusUpdate(userId, "结束充电", responseDTO.getRequestId());
        }
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 取消充电请求
     */
    @PostMapping("/cancel")
    public ResponseEntity<ChargingResponseDTO> cancelCharging() {
        Long userId = getUserIdFromSecurity();
        ChargingResponseDTO responseDTO = chargingRequestService.cancelRequest(userId);
        
        // 如果操作成功，推送状态更新
        if (!"ERROR".equals(responseDTO.getStatus())) {
            pushChargingStatusUpdate(userId, "取消充电", responseDTO.getRequestId());
        }
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 获取用户的活跃充电状态
     */
    @GetMapping("/status/user")
    public ResponseEntity<ChargingStatusDTO> getUserChargingStatus() {
        Long userId = getUserIdFromSecurity();
        ChargingStatusDTO statusDTO = chargingRequestService.getActiveChargingStatus(userId);
        return ResponseEntity.ok(statusDTO);
    }
    
    /**
     * 获取用户电池容量
     */
    @GetMapping("/battery/capacity")
    public ResponseEntity<Map<String, Object>> getBatteryCapacity() {
        Long userId = getUserIdFromSecurity();
        User user = userService.getUserById(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("batteryCapacity", user != null ? user.getBatteryCapacity() : null);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 设置用户电池容量
     */
    @PostMapping("/battery/capacity")
    public ResponseEntity<Map<String, Object>> setBatteryCapacity(@RequestBody Map<String, Double> request) {
        Long userId = getUserIdFromSecurity();
        Double batteryCapacity = request.get("batteryCapacity");
        
        if (batteryCapacity == null || batteryCapacity <= 0) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "电池容量必须大于0");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = userService.updateBatteryCapacity(userId, batteryCapacity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "电池容量设置成功" : "电池容量设置失败");
        
        // 如果设置成功，推送状态更新到前端（如果用户有活跃的充电状态连接）
        if (success) {
            // 检查用户是否有活跃的充电状态，如果有则推送更新
            try {
                ChargingStatusDTO statusDTO = chargingRequestService.getActiveChargingStatus(userId);
                if (statusDTO != null && !"NOT_FOUND".equals(statusDTO.getStatus())) {
                    pushChargingStatusUpdate(userId, "设置电池容量", null);
                }
            } catch (Exception e) {
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 建立SSE连接，获取用户充电状态流
     */
    @GetMapping(value = "/status/stream/user", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamUserChargingStatus(
            @RequestParam(name = "token", required = true) String token) {
        
        Long userId = validateAndSetSecurityContext(token);
        String userKey = "charge_user_" + userId;  // 使用不同的key前缀避免与队列状态连接冲突
        
        SseEmitter emitter = createSseEmitter(userKey, 
            () -> chargingRequestService.getActiveChargingStatus(userId));
        return emitter;
    }
    
    /**
     * 推送充电状态更新的通用方法
     * 与系统其他部分保持一致的SSE推送实现
     * 
     * @param userId 用户ID
     * @param operation 操作名称（用于日志记录）
     * @param requestId 请求ID（可选，用于日志记录）
     */
    private void pushChargingStatusUpdate(Long userId, String operation, String requestId) {
        try {
            String userKey = "charge_user_" + userId;
            ChargingStatusDTO statusDTO = chargingRequestService.getActiveChargingStatus(userId);
            if (statusDTO != null) {
                sseManager.sendToEmitter(userKey, statusDTO);
            }
        } catch (Exception e) {
        }
    }
} 