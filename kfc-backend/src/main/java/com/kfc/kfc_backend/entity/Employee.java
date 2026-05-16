package com.kfc.kfc_backend.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
@Data
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String phone;
    private String idCard;
    private LocalDate hireDate;
    private String position;
    private Double monthlySalary;
}
