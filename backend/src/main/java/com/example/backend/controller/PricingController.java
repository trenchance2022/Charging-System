package com.example.backend.controller;

import com.example.backend.model.dto.PricingInfoDTO;
import com.example.backend.service.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 价格控制器
 * 重构后遵循单一职责原则：只负责处理HTTP请求和响应
 * 遵循依赖倒置原则：依赖抽象接口而非具体实现
 */
@RestController
@RequestMapping("/pricing")
@CrossOrigin(origins = "*")
public class PricingController extends BaseController {
    
    @Autowired
    private PricingService pricingService;
    
    /**
     * 获取当前电价信息
     */
    @GetMapping("/current")
    public ResponseEntity<PricingInfoDTO> getCurrentPricing() {
        try {
            PricingInfoDTO pricingInfo = pricingService.getCurrentPricingInfo();
            return ResponseEntity.ok(pricingInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取所有时段的电价信息
     */
    @GetMapping("/all")
    public ResponseEntity<List<PricingInfoDTO>> getAllPricing() {
        try {
            List<PricingInfoDTO> pricingInfoList = pricingService.getAllPricingInfo();
            return ResponseEntity.ok(pricingInfoList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 建立SSE连接，获取价格信息流
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPricingUpdates(@RequestParam(name = "token", required = true) String token) {
        validateAndSetSecurityContext(token);
        
        String emitterKey = "pricing_" + System.currentTimeMillis();
        SseEmitter emitter = createSseEmitter(emitterKey, 
            () -> pricingService.getCurrentPricingInfo());
        return emitter;
    }
} 