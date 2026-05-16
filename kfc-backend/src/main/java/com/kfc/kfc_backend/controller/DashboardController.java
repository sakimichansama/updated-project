package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(required = false) String month) {
        return dashboardService.getOverview(month);
    }
}

