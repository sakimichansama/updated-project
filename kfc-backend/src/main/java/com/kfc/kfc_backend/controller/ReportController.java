package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.ExcelReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class ReportController {
    @Autowired
    private ExcelReportService excelReportService;

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> export(@PathVariable String type,
                                         @RequestParam(required = false) String month,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        byte[] content = excelReportService.exportReport(type, month, start, end);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + "-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/template/{type}")
    public ResponseEntity<byte[]> template(@PathVariable String type) {
        byte[] content = excelReportService.exportTemplate(type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + "-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/import/{type}")
    public Map<String, Object> importExcel(@PathVariable String type, @RequestParam("file") MultipartFile file) {
        return excelReportService.importReport(type, file);
    }
}

