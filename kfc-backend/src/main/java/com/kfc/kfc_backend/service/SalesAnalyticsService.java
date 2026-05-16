package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.SalesDaily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesAnalyticsService {
    @Autowired
    private SalesService salesService;

    public Map<String, Object> getSalesTrend(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            end = LocalDate.now();
            start = end.minusDays(29);
        }
        List<SalesDaily> records = salesService.findByDateRange(start, end);
        List<Map<String, Object>> dailyList = new ArrayList<>();
        double totalSales = 0.0;
        int totalOrders = 0;
        SalesDaily bestDay = null;
        SalesDaily worstDay = null;

        for (SalesDaily record : records) {
            double amount = record.getTotalAmount() == null ? 0.0 : record.getTotalAmount();
            int orders = record.getOrderCount() == null ? 0 : record.getOrderCount();
            totalSales += amount;
            totalOrders += orders;
            if (bestDay == null || amount > safeAmount(bestDay)) {
                bestDay = record;
            }
            if (worstDay == null || amount < safeAmount(worstDay)) {
                worstDay = record;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("date", record.getSaleDate().toString());
            row.put("totalAmount", round(amount));
            row.put("orderCount", orders);
            row.put("avgTicket", orders == 0 ? 0.0 : round(amount / orders));
            dailyList.add(row);
        }

        long days = Math.max(ChronoUnit.DAYS.between(start, end) + 1, 1);
        LocalDate previousStart = start.minusDays(days);
        LocalDate previousEnd = start.minusDays(1);
        double previousSales = salesService.getTotalSalesBetween(previousStart, previousEnd);
        double growthRate = previousSales == 0 ? (totalSales > 0 ? 100.0 : 0.0) : ((totalSales - previousSales) / previousSales) * 100;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSales", round(totalSales));
        summary.put("totalOrders", totalOrders);
        summary.put("avgDailySales", round(totalSales / days));
        summary.put("avgTicket", totalOrders == 0 ? 0.0 : round(totalSales / totalOrders));
        summary.put("previousSales", round(previousSales));
        summary.put("growthRate", round(growthRate));
        summary.put("bestDay", bestDay == null ? null : bestDay.getSaleDate().toString());
        summary.put("worstDay", worstDay == null ? null : worstDay.getSaleDate().toString());

        List<String> insights = new ArrayList<>();
        insights.add(growthRate >= 0 ? "Sales increased compared with the previous period. Keep the current promotion cadence." : "Sales declined compared with the previous period. Review traffic, average ticket, and promotion execution.");
        if (totalOrders > 0 && totalSales / totalOrders < 30) {
            insights.add("Average ticket is low. Add combo bundles and add-on recommendations.");
        } else {
            insights.add("Average ticket is stable. Continue focusing on high-margin product pairings.");
        }
        if (records.size() < days) {
            insights.add("Sales records are missing in the selected date range, so the trend chart may be incomplete.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("dailyList", dailyList);
        result.put("insights", insights);
        return result;
    }

    private double safeAmount(SalesDaily record) {
        return record.getTotalAmount() == null ? 0.0 : record.getTotalAmount();
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
