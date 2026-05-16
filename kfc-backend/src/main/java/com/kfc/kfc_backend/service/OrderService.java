package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.dto.OrderItemRequest;
import com.kfc.kfc_backend.dto.OrderRequest;
import com.kfc.kfc_backend.entity.OrderItem;
import com.kfc.kfc_backend.entity.OrderRecord;
import com.kfc.kfc_backend.entity.OutStockRecord;
import com.kfc.kfc_backend.entity.Product;
import com.kfc.kfc_backend.entity.SalesDaily;
import com.kfc.kfc_backend.repository.OrderItemRepository;
import com.kfc.kfc_backend.repository.OrderRecordRepository;
import com.kfc.kfc_backend.repository.OutStockRecordRepository;
import com.kfc.kfc_backend.repository.ProductRepository;
import com.kfc.kfc_backend.repository.SalesDailyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {
    @Autowired
    private OrderRecordRepository orderRecordRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OutStockRecordRepository outStockRecordRepository;
    @Autowired
    private SalesDailyRepository salesDailyRepository;

    public List<Map<String, Object>> findByDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);
        return orderRecordRepository.findByOrderTimeBetweenOrderByOrderTimeDesc(startTime, endTime)
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(OrderRequest request) {
        OrderRecord order = new OrderRecord();
        fillOrder(order, request);
        order.setStatus("Completed");
        order = orderRecordRepository.save(order);
        double amount = saveItemsAndOutbound(order, request.getItems());
        if (request.getOrderAmount() == null || request.getOrderAmount() <= 0) {
            order.setOrderAmount(round(Math.max(0, amount - safe(request.getDiscountAmount()))));
            order = orderRecordRepository.save(order);
        }
        adjustSalesDaily(order.getOrderTime().toLocalDate(), 1, order.getOrderAmount());
        return toMap(order);
    }

    @Transactional
    public Map<String, Object> update(Long id, OrderRequest request) {
        OrderRecord order = orderRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("The order does not exist."));
        LocalDate oldDate = order.getOrderTime().toLocalDate();
        double oldAmount = safe(order.getOrderAmount());
        reverseOrderStock(order.getId());
        orderItemRepository.deleteByOrderId(order.getId());
        outStockRecordRepository.deleteByOrderId(order.getId());

        fillOrder(order, request);
        double amount = saveItemsAndOutbound(order, request.getItems());
        if (request.getOrderAmount() == null || request.getOrderAmount() <= 0) {
            order.setOrderAmount(round(Math.max(0, amount - safe(request.getDiscountAmount()))));
        }
        order = orderRecordRepository.save(order);

        adjustSalesDaily(oldDate, -1, -oldAmount);
        adjustSalesDaily(order.getOrderTime().toLocalDate(), 1, order.getOrderAmount());
        return toMap(order);
    }

    @Transactional
    public void delete(Long id) {
        OrderRecord order = orderRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("The order does not exist."));
        reverseOrderStock(order.getId());
        orderItemRepository.deleteByOrderId(order.getId());
        outStockRecordRepository.deleteByOrderId(order.getId());
        orderRecordRepository.deleteById(id);
        adjustSalesDaily(order.getOrderTime().toLocalDate(), -1, -safe(order.getOrderAmount()));
    }

    private void fillOrder(OrderRecord order, OrderRequest request) {
        LocalDateTime orderTime = request.getOrderTime() == null ? LocalDateTime.now() : request.getOrderTime();
        String orderNo = request.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            orderNo = "ORD" + orderTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        }
        order.setOrderNo(orderNo);
        order.setOrderTime(orderTime);
        order.setCustomerName(request.getCustomerName());
        order.setCreatorId(request.getCreatorId() == null || request.getCreatorId() <= 0 ? 1L : request.getCreatorId());
        order.setCreatorName(request.getCreatorName() == null || request.getCreatorName().isBlank() ? "Store Manager" : request.getCreatorName());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDiscountAmount(safe(request.getDiscountAmount()));
        order.setOrderAmount(request.getOrderAmount());
        order.setRemark(request.getRemark());
    }

    private double saveItemsAndOutbound(OrderRecord order, List<OrderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException("The order must contain at least one product.");
        }
        double totalAmount = 0.0;
        for (OrderItemRequest itemRequest : requests) {
            if (itemRequest.getProductId() == null || itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new RuntimeException("Invalid order item.");
            }
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("The product does not exist."));
            int currentStock = product.getStock() == null ? 0 : product.getStock();
            if (currentStock < itemRequest.getQuantity()) {
                throw new RuntimeException(product.getName() + " inventory is insufficient, current inventory:" + currentStock);
            }
            double salePrice = itemRequest.getSalePrice() == null ? safe(product.getSalePrice()) : itemRequest.getSalePrice();
            double costPrice = safe(product.getPurchasePrice());
            double amount = round(salePrice * itemRequest.getQuantity());

            product.setStock(currentStock - itemRequest.getQuantity());
            productRepository.save(product);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(itemRequest.getQuantity());
            item.setSalePrice(salePrice);
            item.setCostPrice(costPrice);
            item.setAmount(amount);
            orderItemRepository.save(item);

            OutStockRecord outStock = new OutStockRecord();
            outStock.setOrderId(order.getId());
            outStock.setProductId(product.getId());
            outStock.setQuantity(itemRequest.getQuantity());
            outStock.setReason("Sales");
            outStock.setCostPrice(costPrice);
            outStock.setCreateTime(order.getOrderTime());
            outStockRecordRepository.save(outStock);
            totalAmount += amount;
        }
        return round(totalAmount);
    }

    private void reverseOrderStock(Long orderId) {
        for (OrderItem item : orderItemRepository.findByOrderId(orderId)) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int stock = product.getStock() == null ? 0 : product.getStock();
                product.setStock(stock + (item.getQuantity() == null ? 0 : item.getQuantity()));
                productRepository.save(product);
            });
        }
    }

    private void adjustSalesDaily(LocalDate date, int orderDelta, double amountDelta) {
        SalesDaily daily = salesDailyRepository.findBySaleDate(date).orElseGet(() -> {
            SalesDaily created = new SalesDaily();
            created.setSaleDate(date);
            created.setOrderCount(0);
            created.setTotalAmount(0.0);
            return created;
        });
        int orderCount = Math.max(0, (daily.getOrderCount() == null ? 0 : daily.getOrderCount()) + orderDelta);
        double totalAmount = Math.max(0, safe(daily.getTotalAmount()) + amountDelta);
        daily.setOrderCount(orderCount);
        daily.setTotalAmount(round(totalAmount));
        salesDailyRepository.save(daily);
    }

    private Map<String, Object> toMap(OrderRecord order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("orderTime", order.getOrderTime());
        map.put("customerName", order.getCustomerName());
        map.put("creatorId", order.getCreatorId());
        map.put("creatorName", order.getCreatorName());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("orderAmount", order.getOrderAmount());
        map.put("discountAmount", order.getDiscountAmount());
        map.put("status", order.getStatus());
        map.put("remark", order.getRemark());
        map.put("items", orderItemRepository.findByOrderId(order.getId()));
        return map;
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
