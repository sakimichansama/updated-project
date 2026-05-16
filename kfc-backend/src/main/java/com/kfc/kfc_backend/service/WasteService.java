package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.Product;
import com.kfc.kfc_backend.entity.OutStockRecord;
import com.kfc.kfc_backend.entity.WasteRecord;
import com.kfc.kfc_backend.repository.OutStockRecordRepository;
import com.kfc.kfc_backend.repository.ProductRepository;
import com.kfc.kfc_backend.repository.WasteRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WasteService {
    @Autowired
    private WasteRecordRepository wasteRecordRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OutStockRecordRepository outStockRecordRepository;

    public List<WasteRecord> findAll() {
        return wasteRecordRepository.findAllByOrderByCreateTimeDesc();
    }

    @Transactional
    public WasteRecord addWaste(Long productId, Integer quantity, String reason) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("The product does not exist."));
        int currentStock = product.getStock() == null ? 0 : product.getStock();
        if (currentStock < quantity) {
            throw new RuntimeException("Insufficient inventory, current inventory:" + currentStock);
        }
        product.setStock(currentStock - quantity);
        productRepository.save(product);

        WasteRecord record = new WasteRecord();
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setReason(reason);
        record.setUnitCost(product.getPurchasePrice() == null ? 0.0 : product.getPurchasePrice());
        record.setStatus("Pending");
        record.setCreateTime(LocalDateTime.now());
        record = wasteRecordRepository.save(record);

        OutStockRecord outStock = new OutStockRecord();
        outStock.setWasteId(record.getId());
        outStock.setProductId(productId);
        outStock.setQuantity(quantity);
        outStock.setReason("Waste");
        outStock.setCostPrice(record.getUnitCost());
        outStock.setCreateTime(record.getCreateTime());
        outStockRecordRepository.save(outStock);
        return record;
    }

    @Transactional
    public WasteRecord closeWaste(Long id, String handler, String action) {
        WasteRecord record = wasteRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("The waste record does not exist."));
        record.setHandler(handler);
        record.setAction(action);
        record.setStatus("Closed");
        record.setClosedTime(LocalDateTime.now());
        return wasteRecordRepository.save(record);
    }

    public Map<String, Object> getWasteStats(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        List<WasteRecord> records = findAll();
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        long totalCount = records.stream().filter(r -> between(r.getCreateTime(), start, end)).count();
        long closedCount = records.stream()
                .filter(r -> between(r.getCreateTime(), start, end))
                .filter(r -> "Closed".equals(r.getStatus()))
                .count();
        int totalQuantity = records.stream()
                .filter(r -> between(r.getCreateTime(), start, end))
                .mapToInt(r -> r.getQuantity() == null ? 0 : r.getQuantity())
                .sum();
        double totalCost = records.stream()
                .filter(r -> between(r.getCreateTime(), start, end))
                .mapToDouble(r -> (r.getQuantity() == null ? 0 : r.getQuantity()) * getWasteUnitCost(r, productMap))
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("closedCount", closedCount);
        result.put("pendingCount", totalCount - closedCount);
        result.put("totalQuantity", totalQuantity);
        result.put("totalCost", Math.round(totalCost * 100) / 100.0);
        result.put("closureRate", totalCount == 0 ? 100.0 : Math.round((closedCount * 10000.0 / totalCount)) / 100.0);
        return result;
    }

    @Transactional
    public void deleteById(Long id) {
        WasteRecord record = wasteRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("The waste record does not exist."));
        productRepository.findById(record.getProductId()).ifPresent(product -> {
            int stock = product.getStock() == null ? 0 : product.getStock();
            product.setStock(stock + (record.getQuantity() == null ? 0 : record.getQuantity()));
            productRepository.save(product);
        });
        outStockRecordRepository.deleteByWasteId(id);
        wasteRecordRepository.deleteById(id);
    }

    private boolean between(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        return time != null && !time.isBefore(start) && !time.isAfter(end);
    }

    private double getWasteUnitCost(WasteRecord record, Map<Long, Product> productMap) {
        if (record.getUnitCost() != null) {
            return record.getUnitCost();
        }
        Product product = productMap.get(record.getProductId());
        return product == null || product.getPurchasePrice() == null ? 0.0 : product.getPurchasePrice();
    }
}
