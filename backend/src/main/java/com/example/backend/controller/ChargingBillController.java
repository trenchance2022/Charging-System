package com.example.backend.controller;

import com.example.backend.model.entity.ChargingBill;
import com.example.backend.service.ChargingBillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bills")
@CrossOrigin(origins = "*")
public class ChargingBillController extends BaseController {
    
    private final ChargingBillService chargingBillService;
    
    public ChargingBillController(ChargingBillService chargingBillService) {
        this.chargingBillService = chargingBillService;
    }
    
    /**
     * 获取当前用户的充电详单列表
     * 
     * @return 充电详单列表
     */
    @GetMapping("/user/current")
    public ResponseEntity<List<ChargingBill>> getCurrentUserBills() {
        Long userId = getUserIdFromSecurity();
        
        try {
            List<ChargingBill> bills = chargingBillService.getBillsByUserId(userId);
            return ResponseEntity.ok(bills);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据详单ID获取充电详单详情
     * 
     * @param billId 详单ID
     * @return 充电详单
     */
    @GetMapping("/{billId}")
    public ResponseEntity<ChargingBill> getBillById(@PathVariable Long billId) {
        try {
            ChargingBill bill = chargingBillService.getById(billId);
            if (bill != null) {
                return ResponseEntity.ok(bill);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 