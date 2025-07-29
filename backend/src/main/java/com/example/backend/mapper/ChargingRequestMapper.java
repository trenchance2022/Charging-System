package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.ChargingRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChargingRequestMapper extends BaseMapper<ChargingRequest> {
    
    /**
     * 查询指定充电模式和前缀下的最大序列号
     * @param chargingMode 充电模式
     * @param prefix 请求ID前缀
     * @return 最大序列号，如果没有找到返回null
     */
    @Select("SELECT MAX(CAST(SUBSTRING(request_id, #{prefixLength}) AS UNSIGNED)) " +
            "FROM charging_request " +
            "WHERE charging_mode = #{chargingMode} " +
            "AND request_id REGEXP CONCAT('^', #{prefix}, '[0-9]+$')")
    Integer selectMaxSequenceByModeAndPrefix(@Param("chargingMode") String chargingMode, 
                                           @Param("prefix") String prefix,
                                           @Param("prefixLength") int prefixLength);
} 