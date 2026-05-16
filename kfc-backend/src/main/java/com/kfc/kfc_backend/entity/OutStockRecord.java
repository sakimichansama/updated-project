package com.kfc.kfc_backend.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "out_stock_record")
@Data
public class OutStockRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private Long orderId;
    private Long wasteId;
    private Integer quantity;
    private String reason;      // Sales, gifting, internal use, loss reporting
    private Double costPrice;   // The cost unit price at the time of outbound (obtained from the product table)
    private LocalDateTime createTime;
}
