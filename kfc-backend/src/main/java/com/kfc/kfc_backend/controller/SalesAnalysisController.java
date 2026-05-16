package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.SalesAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-analysis")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class SalesAnalysisController {
    @Autowired
    private SalesAnalyticsService salesAnalyticsService;

    @GetMapping("/trend")
    public Map<String, Object> trend(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                     @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return salesAnalyticsService.getSalesTrend(start, end);
    }
}

