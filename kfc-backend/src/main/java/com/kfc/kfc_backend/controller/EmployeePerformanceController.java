package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.EmployeePerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employee-performance")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class EmployeePerformanceController {
    @Autowired
    private EmployeePerformanceService employeePerformanceService;

    @GetMapping("/month")
    public Map<String, Object> month(@RequestParam String month) {
        String[] parts = month.split("-");
        return employeePerformanceService.getMonthlyPerformance(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}

