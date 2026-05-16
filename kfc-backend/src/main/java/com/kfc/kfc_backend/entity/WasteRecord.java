package com.kfc.kfc_backend.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waste_record")
@Data
public class WasteRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private Integer quantity;
    private String reason;
    private Double unitCost;
    private String status;
    private String handler;
    private String action;
    private LocalDateTime createTime;
    private LocalDateTime closedTime;
}
