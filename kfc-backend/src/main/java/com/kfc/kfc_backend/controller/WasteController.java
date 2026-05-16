package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.entity.WasteRecord;
import com.kfc.kfc_backend.service.WasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/waste")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class WasteController {
    @Autowired
    private WasteService wasteService;

    @GetMapping
    public List<WasteRecord> list() {
        return wasteService.findAll();
    }

    @PostMapping
    public WasteRecord add(@RequestParam Long productId,
                           @RequestParam Integer quantity,
                           @RequestParam String reason) {
        return wasteService.addWaste(productId, quantity, reason);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestParam String month) {
        String[] parts = month.split("-");
        return wasteService.getWasteStats(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    @PutMapping("/{id}/close")
    public WasteRecord close(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return wasteService.closeWaste(id, body.get("handler"), body.get("action"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        wasteService.deleteById(id);
    }
}

