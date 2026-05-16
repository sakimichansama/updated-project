package com.kfc.kfc_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderRequest {
    private String orderNo;
    private LocalDateTime orderTime;
    private String customerName;
    private Long creatorId;
    private String creatorName;
    private String paymentMethod;
    private Double orderAmount;
    private Double discountAmount;
    private String remark;
    private List<OrderItemRequest> items = new ArrayList<>();
}
