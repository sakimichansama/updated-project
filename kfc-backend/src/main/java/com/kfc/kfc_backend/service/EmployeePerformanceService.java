package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.Employee;
import com.kfc.kfc_backend.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeePerformanceService {
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private WorkHourService workHourService;
    @Autowired
    private PayrollService payrollService;

    public Map<String, Object> getMonthlyPerformance(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        double standardHours = yearMonth.lengthOfMonth() >= 30 ? 176.0 : 160.0;
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Employee employee : employeeRepository.findAll()) {
            double hours = safe(workHourService.getTotalHoursByEmployeeAndMonth(employee.getId(), year, month));
            double baseSalary = employee.getMonthlySalary() == null ? 0.0 : employee.getMonthlySalary();
            double totalSalary = baseSalary;
            double attendanceRate = standardHours == 0 ? 0.0 : Math.min(hours / standardHours * 100, 120);
            double salaryPerHour = hours == 0 ? 0.0 : totalSalary / hours;
            double score = Math.min(attendanceRate, 100) * 0.85 + Math.max(0, 15 - salaryPerHour / 20);

            Map<String, Object> row = new HashMap<>();
            row.put("employeeId", employee.getId());
            row.put("employeeName", employee.getName());
            row.put("position", employee.getPosition());
            row.put("totalHours", round(hours));
            row.put("standardHours", standardHours);
            row.put("attendanceRate", round(attendanceRate));
            row.put("baseSalary", round(baseSalary));
            row.put("totalSalary", round(totalSalary));
            row.put("salaryPerHour", round(salaryPerHour));
            row.put("score", round(score));
            row.put("suggestion", buildSuggestion(hours, standardHours, salaryPerHour));
            rows.add(row);
        }

        rows.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("rank", i + 1);
        }

        double avgScore = rows.stream().mapToDouble(row -> (Double) row.get("score")).average().orElse(0.0);
        double totalLaborCost = payrollService.getTotalLaborCost(year, month);
        Map<String, Object> summary = new HashMap<>();
        summary.put("employeeCount", rows.size());
        summary.put("avgScore", round(avgScore));
        summary.put("totalLaborCost", round(totalLaborCost));
        summary.put("standardHours", standardHours);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("list", rows);
        return result;
    }

    private String buildSuggestion(double hours, double standardHours, double salaryPerHour) {
        if (hours == 0) {
            return "No work hour records this month. Add schedule records or verify attendance.";
        }
        if (hours < standardHours * 0.8) {
            return "Work hours are insufficient. Optimize scheduling or add role training.";
        }
        if (salaryPerHour > 80) {
            return "Labor cost per hour is high. Review scheduling efficiency by role.";
        }
        return "Attendance and labor cost are stable.";
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
