package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.entity.InStockRecord;
import com.kfc.kfc_backend.entity.OutStockRecord;
import com.kfc.kfc_backend.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // POST /api/inventory/in
    @PostMapping("/in")
    public void inStock(@RequestParam Long productId,
                        @RequestParam Integer quantity,
                        @RequestParam Double price,
                        @RequestParam String supplier) {
        inventoryService.inStock(productId, quantity, price, supplier);
    }

    // POST /api/inventory/out
    @PostMapping("/out")
    public void outStock(@RequestParam Long productId,
                         @RequestParam Integer quantity,
                         @RequestParam String reason) {
        inventoryService.outStock(productId, quantity, reason);
    }

    // GET /api/inventory/in-records
    @GetMapping("/in-records")
    public List<InStockRecord> inRecords() {
        return inventoryService.getInRecords();
    }

    // GET /api/inventory/out-records
    @GetMapping("/out-records")
    public List<OutStockRecord> outRecords() {
        return inventoryService.getOutRecords();
    }

    @DeleteMapping("/in-records/{id}")
    public void deleteInRecord(@PathVariable Long id) {
        inventoryService.deleteInRecord(id);
    }

    @DeleteMapping("/out-records/{id}")
    public void deleteOutRecord(@PathVariable Long id) {
        inventoryService.deleteOutRecord(id);
    }

    @GetMapping("/alerts")
    public List<Map<String, Object>> alerts() {
        return inventoryService.getInventoryAlerts();
    }

    @GetMapping("/replenishment-suggestions")
    public List<Map<String, Object>> replenishmentSuggestions() {
        return inventoryService.getReplenishmentSuggestions();
    }
}

