package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.Product;
import com.kfc.kfc_backend.repository.EmployeeRepository;
import com.kfc.kfc_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ProfitService profitService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private SalesAnalyticsService salesAnalyticsService;
    @Autowired
    private EmployeePerformanceService employeePerformanceService;
    @Autowired
    private WasteService wasteService;

    public Map<String, Object> getOverview(String month) {
        YearMonth yearMonth = parseMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        Map<String, Object> profitReport = profitService.getMonthlyProfitReport(yearMonth.getYear(), yearMonth.getMonthValue());
        Map<String, Object> profitSummary = (Map<String, Object>) profitReport.get("summary");
        Map<String, Object> salesTrend = salesAnalyticsService.getSalesTrend(start, end);
        Map<String, Object> salesSummary = (Map<String, Object>) salesTrend.get("summary");
        Map<String, Object> performance = employeePerformanceService.getMonthlyPerformance(yearMonth.getYear(), yearMonth.getMonthValue());
        Map<String, Object> performanceSummary = (Map<String, Object>) performance.get("summary");
        Map<String, Object> wasteStats = wasteService.getWasteStats(yearMonth.getYear(), yearMonth.getMonthValue());

        List<Product> products = productRepository.findAll();
        double stockValue = products.stream()
                .mapToDouble(p -> (p.getStock() == null ? 0 : p.getStock()) * (p.getPurchasePrice() == null ? 0.0 : p.getPurchasePrice()))
                .sum();
        List<Map<String, Object>> alerts = inventoryService.getInventoryAlerts();

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalSales", profitSummary.get("totalSales"));
        kpis.put("netProfit", profitSummary.get("netProfit"));
        kpis.put("profitMargin", profitSummary.get("profitMargin"));
        kpis.put("avgDailySales", salesSummary.get("avgDailySales"));
        kpis.put("totalOrders", salesSummary.get("totalOrders"));
        kpis.put("productCount", products.size());
        kpis.put("lowStockCount", alerts.size());
        kpis.put("stockValue", round(stockValue));
        kpis.put("employeeCount", employeeRepository.count());
        kpis.put("avgEmployeeScore", performanceSummary.get("avgScore"));
        kpis.put("wasteCost", wasteStats.get("totalCost"));
        kpis.put("wasteClosureRate", wasteStats.get("closureRate"));

        List<String> insights = new ArrayList<>();
        double profitMargin = toDouble(profitSummary.get("profitMargin"));
        if (profitMargin < 10) {
            insights.add("Net margin is below 10%. Review material cost, waste cost, and labor scheduling first.");
        } else {
            insights.add("Net margin is within a controllable range. Continue tracking the sales mix of high-margin products.");
        }
        if (!alerts.isEmpty()) {
            insights.add("There are " + alerts.size() + " inventory alert products. Purchase first based on replenishment advice.");
        }
        if (toDouble(wasteStats.get("closureRate")) < 90) {
            insights.add("Waste closure rate is below 90%. Complete handler and corrective action details promptly.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("month", yearMonth.toString());
        result.put("kpis", kpis);
        result.put("salesTrend", salesTrend.get("dailyList"));
        result.put("profitDailyList", profitReport.get("dailyList"));
        result.put("costBreakdown", profitReport.get("costBreakdown"));
        result.put("inventoryAlerts", alerts);
        result.put("employeePerformance", performance.get("list"));
        result.put("wasteStats", wasteStats);
        result.put("insights", insights);
        return result;
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        return YearMonth.parse(month);
    }

    private double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
