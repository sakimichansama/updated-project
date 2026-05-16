package com.kfc.kfc_backend.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "in_stock_record")
@Data
public class InStockRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private Integer quantity;
    private Double price;
    private String supplier;
    private LocalDateTime createTime;
}
