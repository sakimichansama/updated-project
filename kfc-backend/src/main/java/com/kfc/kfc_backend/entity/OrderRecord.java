package com.kfc.kfc_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_record")
@Data
public class OrderRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String orderNo;
    private LocalDateTime orderTime;
    private String customerName;
    private Long creatorId;
    private String creatorName;
    private String paymentMethod;
    private Double orderAmount;
    private Double discountAmount;
    private String status;
    private String remark;
}
