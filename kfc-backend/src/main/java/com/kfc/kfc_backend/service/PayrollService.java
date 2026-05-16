package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.Employee;
import com.kfc.kfc_backend.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollService {
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private WorkHourService workHourService;

    public List<Map<String, Object>> getMonthlyPayroll(int year, int month) {
        List<Employee> employees = employeeRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : employees) {
            Double totalHours = workHourService.getTotalHoursByEmployeeAndMonth(emp.getId(), year, month);
            if (totalHours == null) totalHours = 0.0;
            double baseSalary = emp.getMonthlySalary() != null ? emp.getMonthlySalary() : 0.0;
            double totalSalary = baseSalary;

            Map<String, Object> row = new HashMap<>();
            row.put("employeeName", emp.getName());
            row.put("totalHours", totalHours);
            row.put("baseSalary", baseSalary);
            row.put("totalSalary", totalSalary);
            result.add(row);
        }
        return result;
    }

    public double getTotalLaborCost(int year, int month) {
        List<Map<String, Object>> payroll = getMonthlyPayroll(year, month);
        return payroll.stream().mapToDouble(m -> (Double) m.get("totalSalary")).sum();
    }
}
