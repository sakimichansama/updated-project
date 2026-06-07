package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.ProfitService;
import com.kfc.kfc_backend.service.ExcelReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/profit")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class ProfitController {

    @Autowired
    private ProfitService profitService;
    @Autowired
    private ExcelReportService excelReportService;

    // GET /api/profit/month?month=yyyy-MM
    @GetMapping("/month")
    public Map<String, Object> getMonthlyProfit(@RequestParam String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int monthNum = Integer.parseInt(parts[1]);
        return profitService.getMonthlyProfitReport(year, monthNum);
    }

    // GET /api/profit/product-margin?start=yyyy-MM-dd&end=yyyy-MM-dd
    @GetMapping("/product-margin")
    public Map<String, Object> getProductMargin(@RequestParam String start, @RequestParam String end) {
        return profitService.getProductMarginReport(LocalDate.parse(start), LocalDate.parse(end));
    }

    // GET /api/profit/export?month=yyyy-MM
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam String month) {
        byte[] content = excelReportService.exportReport("profit", month, null, null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profit-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}

