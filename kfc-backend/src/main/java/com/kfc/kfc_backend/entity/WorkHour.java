package com.kfc.kfc_backend.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "work_hour")
@Data
public class WorkHour {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long employeeId;
    private LocalDate workDate;
    private Double hours;   // Hours
}
