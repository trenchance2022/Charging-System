package com.example.backend.controller;

import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.SystemConfig;
import com.example.backend.service.ChargingPileService;
import com.example.backend.service.ChargingBillService;
import com.example.backend.service.ChargingRequestService;
import com.example.backend.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 管理员控制器
 * 处理管理员相关的请求，如充电桩状态查询等
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController extends BaseController {
    
    private final ChargingPileService chargingPileService;
    private final ChargingBillService chargingBillService;
    private final ChargingRequestService chargingRequestService;
    private final SystemConfigService systemConfigService;
    
    public AdminController(ChargingPileService chargingPileService, 
                          ChargingBillService chargingBillService,
                          ChargingRequestService chargingRequestService,
                          SystemConfigService systemConfigService) {
        this.chargingPileService = chargingPileService;
        this.chargingBillService = chargingBillService;
        this.chargingRequestService = chargingRequestService;
        this.systemConfigService = systemConfigService;
    }
    
    /**
     * 获取系统配置
     * @return 系统配置信息
     */
    @GetMapping("/system-config")
    public ResponseEntity<List<Map<String, Object>>> getSystemConfig() {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            List<SystemConfig> configs = systemConfigService.getAllConfigs();
            
            // 将配置转换为前端需要的格式
            List<Map<String, Object>> configList = configs.stream()
                .map(config -> {
                    Map<String, Object> configInfo = new HashMap<>();
                    configInfo.put("key", config.getConfigKey());
                    configInfo.put("value", config.getConfigValue());
                    configInfo.put("description", config.getDescription());
                    configInfo.put("displayName", getDisplayName(config.getConfigKey()));
                    configInfo.put("type", getConfigType(config.getConfigKey()));
                    return configInfo;
                })
                .toList();
            
            return ResponseEntity.ok(configList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * 更新系统配置
     * @param configMap 配置键值对映射
     * @return 操作结果
     */
    @PostMapping("/system-config")
    public ResponseEntity<Map<String, Object>> updateSystemConfig(@RequestBody Map<String, String> configMap) {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            boolean success = systemConfigService.updateConfigs(configMap);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "系统配置更新成功" : "系统配置更新失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "系统配置更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有充电桩的状态信息
     * @return 充电桩状态列表
     */
    @GetMapping("/piles/status")
    public ResponseEntity<List<Map<String, Object>>> getPileStatus() {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            List<ChargingPile> piles = chargingPileService.getAllPiles();
            
            // 将充电桩信息转换为前端需要的格式
            List<Map<String, Object>> pileStatusList = piles.stream()
                .map(pile -> {
                    Map<String, Object> pileInfo = new HashMap<>();
                    pileInfo.put("id", pile.getPileNumber()); // 使用pileNumber作为id
                    pileInfo.put("type", "FAST".equals(pile.getPileType()) ? "fast" : "slow");
                    pileInfo.put("isWorking", "AVAILABLE".equals(pile.getStatus()) || "CHARGING".equals(pile.getStatus()));
                    pileInfo.put("power", pile.getChargingPower());
                    pileInfo.put("status", pile.getStatus());
                    
                    // 获取累计统计信息
                    Map<String, Object> statistics = chargingBillService.getPileStatistics(pile.getPileNumber());
                    pileInfo.put("totalCharges", statistics.get("totalCharges"));
                    pileInfo.put("totalTime", statistics.get("totalTime"));
                    pileInfo.put("totalPower", statistics.get("totalPower"));
                    
                    return pileInfo;
                })
                .toList();
            
            return ResponseEntity.ok(pileStatusList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * 获取单个充电桩的详细信息
     * @param pileId 充电桩编号
     * @return 充电桩详细信息
     */
    @GetMapping("/pile/{pileId}")
    public ResponseEntity<Map<String, Object>> getPileDetail(@PathVariable String pileId) {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            // 通过pileNumber查找充电桩
            List<ChargingPile> allPiles = chargingPileService.getAllPiles();
            ChargingPile pile = allPiles.stream()
                .filter(p -> pileId.equals(p.getPileNumber()))
                .findFirst()
                .orElse(null);
            
            if (pile == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 获取累计统计信息
            Map<String, Object> statistics = chargingBillService.getPileStatistics(pileId);
            
            // 获取排队车辆信息
            List<Map<String, Object>> queueInfo = chargingRequestService.getPileQueueInfo(pile.getId());
            
            // 组装详情信息
            Map<String, Object> detail = new HashMap<>();
            detail.put("isWorking", "AVAILABLE".equals(pile.getStatus()) || "CHARGING".equals(pile.getStatus()));
            detail.put("totalCharges", statistics.get("totalCharges"));
            detail.put("totalTime", statistics.get("totalTime"));
            detail.put("totalPower", statistics.get("totalPower"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("detail", detail);
            response.put("queue", queueInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 切换充电桩状态
     * @param pileId 充电桩编号
     * @return 操作结果
     */
    @PostMapping("/pile/{pileId}/toggle")
    public ResponseEntity<Map<String, Object>> togglePileStatus(@PathVariable String pileId) {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            boolean success = chargingPileService.togglePileStatus(pileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "状态切换成功" : "状态切换失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "状态切换失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取配置项的显示名称
     */
    private String getDisplayName(String configKey) {
        switch (configKey) {
            case "FastChargingPileNum":
                return "快充电桩数";
            case "TrickleChargingPileNum":
                return "慢充电桩数";
            case "WaitingAreaSize":
                return "等候区车位容量";
            case "ChargingQueueLen":
                return "充电桩排队队列长度";
            case "schedule_strategy":
                return "调度算法策略";
            default:
                return configKey;
        }
    }
    
    /**
     * 获取配置项的类型
     */
    private String getConfigType(String configKey) {
        switch (configKey) {
            case "FastChargingPileNum":
            case "TrickleChargingPileNum":
            case "WaitingAreaSize":
            case "ChargingQueueLen":
                return "number";
            case "schedule_strategy":
                return "select";
            default:
                return "text";
        }
    }
    
    /**
     * 获取报表统计数据
     * @param startDate 开始日期 (格式: yyyy-MM-dd)
     * @param endDate 结束日期 (格式: yyyy-MM-dd)
     * @return 报表统计数据
     */
    @GetMapping("/report")
    public ResponseEntity<List<Map<String, Object>>> getReportStatistics(
            @RequestParam String startDate, 
            @RequestParam String endDate) {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            List<Map<String, Object>> reportData = chargingBillService.getReportStatistics(startDate, endDate);
            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * 添加新的充电桩
     * @param pileType 充电桩类型（FAST或SLOW）
     * @return 操作结果
     */
    @PostMapping("/piles/add")
    public ResponseEntity<Map<String, Object>> addChargingPile(@RequestParam String pileType) {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            ChargingPile newPile = chargingPileService.addChargingPile(pileType);
            
            Map<String, Object> response = new HashMap<>();
            if (newPile != null) {
                response.put("success", true);
                response.put("message", "充电桩添加成功");
                response.put("pileNumber", newPile.getPileNumber());
                response.put("pile", convertPileToMap(newPile));
            } else {
                response.put("success", false);
                response.put("message", "充电桩添加失败");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "充电桩添加失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除充电桩
     * @param pileNumber 充电桩编号
     * @return 操作结果
     */
    @DeleteMapping("/piles/{pileNumber}")
    public ResponseEntity<Map<String, Object>> deleteChargingPile(@PathVariable String pileNumber) {
        try {
            // 检查是否有管理员权限
            if (!hasAuthority("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            
            boolean success = chargingPileService.deleteChargingPile(pileNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "充电桩删除成功" : "充电桩删除失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "充电桩删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 将充电桩实体转换为前端需要的格式
     */
    private Map<String, Object> convertPileToMap(ChargingPile pile) {
        Map<String, Object> pileInfo = new HashMap<>();
        pileInfo.put("id", pile.getPileNumber());
        pileInfo.put("type", "FAST".equals(pile.getPileType()) ? "fast" : "slow");
        pileInfo.put("isWorking", "AVAILABLE".equals(pile.getStatus()) || "CHARGING".equals(pile.getStatus()));
        pileInfo.put("power", pile.getChargingPower());
        pileInfo.put("status", pile.getStatus());
        
        // 获取累计统计信息
        Map<String, Object> statistics = chargingBillService.getPileStatistics(pile.getPileNumber());
        pileInfo.put("totalCharges", statistics.get("totalCharges"));
        pileInfo.put("totalTime", statistics.get("totalTime"));
        pileInfo.put("totalPower", statistics.get("totalPower"));
        
        return pileInfo;
    }
} 