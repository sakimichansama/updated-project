package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.entity.Employee;
import com.kfc.kfc_backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // GET /api/employees
    @GetMapping
    public List<Employee> list() {
        return employeeService.findAll();
    }

    // POST /api/employees
    @PostMapping
    public Employee add(@RequestBody Employee employee) {
        return employeeService.save(employee);
    }

    // PUT /api/employees/{id}
    @PutMapping("/{id}")
    public Employee update(@PathVariable Long id, @RequestBody Employee employee) {
        employee.setId(id);
        return employeeService.save(employee);
    }

    // DELETE /api/employees/{id}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        employeeService.deleteById(id);
    }
}

