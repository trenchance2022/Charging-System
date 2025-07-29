package com.example.backend.service;

/**
 * 系统常量服务接口
 * 负责从数据库加载常量并在内存中管理
 */
public interface SystemConstantService {
    
    /**
     * 从数据库加载所有常量到内存
     */
    void loadConstantsFromDatabase();
    
    /**
     * 通用常量获取方法 - 自动根据数据库中的类型返回相应类型的值
     * @param key 常量键
     * @return 常量值，自动转换为正确的类型，如果不存在返回null
     */
    Object getConstant(String key);
    
    /**
     * 更新内存中的常量值
     * @param key 常量键
     * @param value 新值
     */
    void updateConstantInMemory(String key, String value);
} 