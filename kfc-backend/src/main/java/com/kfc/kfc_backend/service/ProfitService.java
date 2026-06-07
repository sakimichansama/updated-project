package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.OutStockRecord;
import com.kfc.kfc_backend.entity.OrderItem;
import com.kfc.kfc_backend.entity.OrderRecord;
import com.kfc.kfc_backend.entity.Product;
import com.kfc.kfc_backend.entity.SalesDaily;
import com.kfc.kfc_backend.repository.OrderItemRepository;
import com.kfc.kfc_backend.repository.OrderRecordRepository;
import com.kfc.kfc_backend.repository.OutStockRecordRepository;
import com.kfc.kfc_backend.repository.ProductRepository;
import com.kfc.kfc_backend.repository.WasteRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class ProfitService {
    @Autowired
    private OutStockRecordRepository outStockRecordRepository;
    @Autowired
    private OrderRecordRepository orderRecordRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SalesService salesService;
    @Autowired
    private PayrollService payrollService;
    @Autowired
    private WasteRecordRepository wasteRecordRepository;

    public Map<String, Object> getMonthlyProfitReport(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Total sales
        double totalSales = salesService.getTotalSalesBetween(startDate, endDate);

        // Material cost: product cost of sales outbound records in the month
        List<OutStockRecord> salesOutRecords = outStockRecordRepository.findSalesOutStockBetween(startDateTime, endDateTime);
        double materialCost = salesOutRecords.stream()
                .mapToDouble(r -> r.getQuantity() * r.getCostPrice())
                .sum();

        // Waste cost
        Double wasteCost = wasteRecordRepository.sumWasteCostBetween(startDateTime, endDateTime);
        if (wasteCost == null) wasteCost = 0.0;

        // Labor cost
        double laborCost = payrollService.getTotalLaborCost(year, month);

        double grossProfit = totalSales - materialCost;
        double netProfit = grossProfit - laborCost - wasteCost;
        double profitMargin = totalSales == 0 ? 0 : (netProfit / totalSales) * 100;

        // Daily profit, excluding labor because labor cannot be allocated by day
        List<SalesDaily> dailySales = salesService.findByDateRange(startDate, endDate);
        List<Map<String, Object>> dailyList = new ArrayList<>();
        for (SalesDaily day : dailySales) {
            LocalDate date = day.getSaleDate();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            List<OutStockRecord> daySalesOut = outStockRecordRepository.findSalesOutStockBetween(dayStart, dayEnd);
            double dayMaterialCost = daySalesOut.stream().mapToDouble(r -> r.getQuantity() * r.getCostPrice()).sum();
            double dayProfit = day.getTotalAmount() - dayMaterialCost;
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("profit", Math.round(dayProfit * 100) / 100.0);
            dailyList.add(item);
        }

        // Cost breakdown
        Map<String, Object> costBreakdown = new HashMap<>();
        costBreakdown.put("materialCost", Math.round(materialCost * 100) / 100.0);
        costBreakdown.put("laborCost", Math.round(laborCost * 100) / 100.0);
        costBreakdown.put("wasteCost", Math.round(wasteCost * 100) / 100.0);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSales", Math.round(totalSales * 100) / 100.0);
        summary.put("materialCost", Math.round(materialCost * 100) / 100.0);
        summary.put("laborCost", Math.round(laborCost * 100) / 100.0);
        summary.put("wasteCost", Math.round(wasteCost * 100) / 100.0);
        summary.put("grossProfit", Math.round(grossProfit * 100) / 100.0);
        summary.put("netProfit", Math.round(netProfit * 100) / 100.0);
        summary.put("profitMargin", Math.round(profitMargin * 100) / 100.0);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("dailyList", dailyList);
        result.put("costBreakdown", costBreakdown);
        return result;
    }

    public Map<String, Object> getProductMarginReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Map<Long, Product> products = new HashMap<>();
        for (Product product : productRepository.findAll()) {
            products.put(product.getId(), product);
        }

        Map<Long, MarginAccumulator> productStats = new HashMap<>();
        Map<String, MarginAccumulator> categoryStats = new HashMap<>();
        for (OrderRecord order : orderRecordRepository.findByOrderTimeBetweenOrderByOrderTimeDesc(startDateTime, endDateTime)) {
            if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
                continue;
            }
            for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                Long productId = item.getProductId();
                Product product = products.get(productId);
                String productName = firstNonBlank(item.getProductName(), product == null ? null : product.getName(), "Unknown Product");
                String category = firstNonBlank(product == null ? null : product.getCategory(), "Uncategorized");
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                double salesAmount = item.getAmount() == null ? safe(item.getSalePrice()) * quantity : item.getAmount();
                double costAmount = safe(item.getCostPrice()) * quantity;

                productStats.computeIfAbsent(productId == null ? -1L : productId,
                                id -> new MarginAccumulator(productId, productName, category))
                        .add(quantity, salesAmount, costAmount);
                categoryStats.computeIfAbsent(category,
                                name -> new MarginAccumulator(null, name, name))
                        .add(quantity, salesAmount, costAmount);
            }
        }

        List<Map<String, Object>> productList = productStats.values().stream()
                .map(MarginAccumulator::toProductMap)
                .sorted(this::compareByGrossProfit)
                .toList();
        List<Map<String, Object>> categoryList = categoryStats.values().stream()
                .map(MarginAccumulator::toCategoryMap)
                .sorted(this::compareByGrossProfit)
                .toList();

        double totalSales = productStats.values().stream().mapToDouble(stat -> stat.salesAmount).sum();
        double totalCost = productStats.values().stream().mapToDouble(stat -> stat.costAmount).sum();
        double grossProfit = totalSales - totalCost;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSales", round(totalSales));
        summary.put("totalCost", round(totalCost));
        summary.put("grossProfit", round(grossProfit));
        summary.put("grossMargin", totalSales == 0 ? 0.0 : round(grossProfit / totalSales * 100));
        summary.put("productCount", productList.size());
        summary.put("categoryCount", categoryList.size());
        summary.put("topProduct", productList.isEmpty() ? "-" : productList.get(0).get("productName"));
        summary.put("topCategory", categoryList.isEmpty() ? "-" : categoryList.get(0).get("category"));

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("summary", summary);
        result.put("productList", productList);
        result.put("categoryList", categoryList);
        return result;
    }

    private int compareByGrossProfit(Map<String, Object> a, Map<String, Object> b) {
        return Double.compare((Double) b.get("grossProfit"), (Double) a.get("grossProfit"));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static class MarginAccumulator {
        private final Long productId;
        private final String name;
        private final String category;
        private int quantity;
        private double salesAmount;
        private double costAmount;

        private MarginAccumulator(Long productId, String name, String category) {
            this.productId = productId;
            this.name = name;
            this.category = category;
        }

        private void add(int quantity, double salesAmount, double costAmount) {
            this.quantity += quantity;
            this.salesAmount += salesAmount;
            this.costAmount += costAmount;
        }

        private Map<String, Object> toProductMap() {
            Map<String, Object> item = baseMap();
            item.put("productId", productId);
            item.put("productName", name);
            item.put("category", category);
            return item;
        }

        private Map<String, Object> toCategoryMap() {
            Map<String, Object> item = baseMap();
            item.put("category", name);
            return item;
        }

        private Map<String, Object> baseMap() {
            double grossProfit = salesAmount - costAmount;
            Map<String, Object> item = new HashMap<>();
            item.put("quantity", quantity);
            item.put("salesAmount", Math.round(salesAmount * 100) / 100.0);
            item.put("costAmount", Math.round(costAmount * 100) / 100.0);
            item.put("grossProfit", Math.round(grossProfit * 100) / 100.0);
            item.put("grossMargin", salesAmount == 0 ? 0.0 : Math.round((grossProfit / salesAmount * 100) * 100) / 100.0);
            return item;
        }
    }
}
