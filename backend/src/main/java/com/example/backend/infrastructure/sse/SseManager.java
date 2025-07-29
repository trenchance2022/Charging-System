package com.example.backend.infrastructure.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Supplier;

/**
 * SSE管理器接口
 * 遵循单一职责原则：专门处理SSE相关的操作
 * 遵循开闭原则：通过抽象接口，对扩展开放，对修改封闭
 */
public interface SseManager {
    
    /**
     * 创建SSE发射器
     * @param key 发射器的唯一标识
     * @param statusSupplier 状态数据提供者
     * @param <T> 状态数据类型
     * @return SSE发射器
     */
    <T> SseEmitter createEmitter(String key, Supplier<T> statusSupplier);
    
    /**
     * 向指定的发射器发送数据
     * @param key 发射器标识
     * @param data 要发送的数据
     * @param <T> 数据类型
     */
    <T> void sendToEmitter(String key, T data);
    
    /**
     * 移除发射器
     * @param key 发射器标识
     */
    void removeEmitter(String key);
    
    /**
     * 向所有发射器广播数据
     * @param data 要广播的数据
     * @param <T> 数据类型
     */
    <T> void broadcast(T data);
} 