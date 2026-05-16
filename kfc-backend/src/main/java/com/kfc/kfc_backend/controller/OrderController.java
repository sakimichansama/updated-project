package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.dto.OrderRequest;
import com.kfc.kfc_backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return orderService.findByDateRange(start, end);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody OrderRequest request) {
        return orderService.create(request);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody OrderRequest request) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}
