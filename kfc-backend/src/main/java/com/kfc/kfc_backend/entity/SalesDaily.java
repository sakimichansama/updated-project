package com.kfc.kfc_backend.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "sales_daily")
@Data
public class SalesDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate saleDate;
    private Integer orderCount;
    private Double totalAmount;
}
