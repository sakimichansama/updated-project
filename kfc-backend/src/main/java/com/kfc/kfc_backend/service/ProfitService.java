package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.OutStockRecord;
import com.kfc.kfc_backend.entity.SalesDaily;
import com.kfc.kfc_backend.repository.OutStockRecordRepository;
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
}
