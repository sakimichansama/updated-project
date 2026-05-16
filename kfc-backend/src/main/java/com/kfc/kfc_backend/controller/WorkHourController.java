package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.WorkHourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/workhours")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class WorkHourController {

    @Autowired
    private WorkHourService workHourService;

    // POST /api/workhours?employeeId=1&workDate=2026-04-15&hours=8.0
    @PostMapping
    public void addWorkHour(@RequestParam Long employeeId,
                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate workDate,
                            @RequestParam Double hours) {
        workHourService.addWorkHour(employeeId, workDate, hours);
    }
}

