package com.kfc.kfc_backend.entity;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "product")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String specification;
    private String category;
    private String unit;
    private Double purchasePrice;
    private Double salePrice;
    private Integer stock;
    private Integer minStock;
}
