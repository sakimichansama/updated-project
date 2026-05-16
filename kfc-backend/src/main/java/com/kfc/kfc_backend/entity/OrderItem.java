package com.kfc.kfc_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "order_item")
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double salePrice;
    private Double costPrice;
    private Double amount;
}
