package com.kfc.kfc_backend.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long productId;
    private Integer quantity;
    private Double salePrice;
}
