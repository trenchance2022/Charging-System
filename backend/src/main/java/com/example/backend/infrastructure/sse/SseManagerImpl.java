package com.example.backend.infrastructure.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * SSE管理器实现类
 * 遵循单一职责原则：专门处理SSE相关的操作
 */
@Component
public class SseManagerImpl implements SseManager {
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    @Override
    public <T> SseEmitter createEmitter(String key, Supplier<T> statusSupplier) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        // 设置回调
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(key);
        });
        
        emitter.onCompletion(() -> {
            removeEmitter(key);
        });
        
        emitter.onError(e -> {
            removeEmitter(key);
        });
        
        // 保存发射器
        emitters.put(key, emitter);
        
        // 立即发送一次状态
        try {
            T statusDTO = statusSupplier.get();
            emitter.send(statusDTO);
        } catch (IOException e) {
            emitter.completeWithError(e);
            removeEmitter(key);
        }
        
        return emitter;
    }
    
    @Override
    public <T> void sendToEmitter(String key, T data) {
        SseEmitter emitter = emitters.get(key);
        if (emitter != null) {
            try {
                emitter.send(data);
            } catch (IOException e) {
                emitter.completeWithError(e);
                removeEmitter(key);
            }
        }
    }
    
    @Override
    public void removeEmitter(String key) {
        emitters.remove(key);
    }
    
    @Override
    public <T> void broadcast(T data) {
        emitters.entrySet().removeIf(entry -> {
            try {
                entry.getValue().send(data);
                return false; // 发送成功，保留连接
            } catch (IOException e) {
                entry.getValue().completeWithError(e);
                return true; // 发送失败，移除连接
            }
        });
    }
} 