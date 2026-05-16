package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    // GET /api/payroll/month?month=yyyy-MM
    @GetMapping("/month")
    public List<Map<String, Object>> getMonthlyPayroll(@RequestParam String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int monthNum = Integer.parseInt(parts[1]);
        return payrollService.getMonthlyPayroll(year, monthNum);
    }
}

