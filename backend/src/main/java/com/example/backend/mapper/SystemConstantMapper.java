package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.SystemConstant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统常量数据访问接口
 */
@Mapper
public interface SystemConstantMapper extends BaseMapper<SystemConstant> {
} 