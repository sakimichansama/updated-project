package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.InStockRecord;
import com.kfc.kfc_backend.entity.OutStockRecord;
import com.kfc.kfc_backend.entity.Product;
import com.kfc.kfc_backend.repository.InStockRecordRepository;
import com.kfc.kfc_backend.repository.OutStockRecordRepository;
import com.kfc.kfc_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InStockRecordRepository inStockRecordRepository;
    @Autowired
    private OutStockRecordRepository outStockRecordRepository;

    @Transactional
    public void inStock(Long productId, Integer quantity, Double price, String supplier) {
        validateQuantity(quantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("The product does not exist."));
        product.setStock(safeStock(product) + quantity);
        productRepository.save(product);

        InStockRecord record = new InStockRecord();
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setPrice(price);
        record.setSupplier(supplier);
        record.setCreateTime(LocalDateTime.now());
        inStockRecordRepository.save(record);
    }

    @Transactional
    public void outStock(Long productId, Integer quantity, String reason) {
        validateQuantity(quantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("The product does not exist."));
        int currentStock = safeStock(product);
        if (currentStock < quantity) {
            throw new RuntimeException("Insufficient inventory, current inventory:" + currentStock);
        }
        product.setStock(currentStock - quantity);
        productRepository.save(product);

        OutStockRecord record = new OutStockRecord();
        record.setProductId(productId);
        record.setQuantity(quantity);
        record.setReason(reason);
        record.setCostPrice(product.getPurchasePrice());
        record.setCreateTime(LocalDateTime.now());
        outStockRecordRepository.save(record);
    }

    public List<InStockRecord> getInRecords() {
        return inStockRecordRepository.findAllByOrderByCreateTimeDesc();
    }

    public List<OutStockRecord> getOutRecords() {
        return outStockRecordRepository.findAllByOrderByCreateTimeDesc();
    }

    @Transactional
    public void deleteInRecord(Long id) {
        InStockRecord record = inStockRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("The inbound record does not exist."));
        Product product = productRepository.findById(record.getProductId())
                .orElseThrow(() -> new RuntimeException("The product does not exist."));
        int currentStock = safeStock(product);
        int quantity = record.getQuantity() == null ? 0 : record.getQuantity();
        if (currentStock < quantity) {
            throw new RuntimeException("Cannot delete inbound record because current inventory is insufficient.");
        }
        product.setStock(currentStock - quantity);
        productRepository.save(product);
        inStockRecordRepository.deleteById(id);
    }

    @Transactional
    public void deleteOutRecord(Long id) {
        OutStockRecord record = outStockRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("The outbound record does not exist."));
        if (record.getOrderId() != null) {
            throw new RuntimeException("Order outbound records must be adjusted from the order page.");
        }
        if (record.getWasteId() != null) {
            throw new RuntimeException("Waste outbound records must be adjusted from the waste page.");
        }
        Product product = productRepository.findById(record.getProductId())
                .orElseThrow(() -> new RuntimeException("The product does not exist."));
        int currentStock = safeStock(product);
        int quantity = record.getQuantity() == null ? 0 : record.getQuantity();
        product.setStock(currentStock + quantity);
        productRepository.save(product);
        outStockRecordRepository.deleteById(id);
    }

    public List<Map<String, Object>> getInventoryAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            int stock = safeStock(product);
            int minStock = product.getMinStock() == null ? 0 : product.getMinStock();
            if (stock <= minStock) {
                Map<String, Object> item = buildSuggestion(product, stock, minStock);
                item.put("level", stock == 0 ? "critical" : "warning");
                alerts.add(item);
            }
        }
        alerts.sort(Comparator.comparingInt(item -> (Integer) item.get("stock")));
        return alerts;
    }

    public List<Map<String, Object>> getReplenishmentSuggestions() {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            int stock = safeStock(product);
            int minStock = product.getMinStock() == null ? 0 : product.getMinStock();
            Map<String, Object> item = buildSuggestion(product, stock, minStock);
            item.put("level", stock <= minStock ? (stock == 0 ? "critical" : "warning") : "normal");
            suggestions.add(item);
        }
        suggestions.sort((a, b) -> {
            double aRatio = (Double) a.get("stockRatio");
            double bRatio = (Double) b.get("stockRatio");
            return Double.compare(aRatio, bRatio);
        });
        return suggestions;
    }

    private Map<String, Object> buildSuggestion(Product product, int stock, int minStock) {
        int targetStock = Math.max(minStock * 2, minStock + 10);
        int suggestedQuantity = Math.max(targetStock - stock, 0);
        double stockRatio = minStock == 0 ? (stock > 0 ? 999.0 : 0.0) : Math.round((stock * 1.0 / minStock) * 100.0) / 100.0;

        Map<String, Object> item = new HashMap<>();
        item.put("productId", product.getId());
        item.put("productName", product.getName());
        item.put("category", product.getCategory());
        item.put("unit", product.getUnit());
        item.put("stock", stock);
        item.put("minStock", minStock);
        item.put("shortage", Math.max(minStock - stock, 0));
        item.put("targetStock", targetStock);
        item.put("suggestedQuantity", suggestedQuantity);
        item.put("estimatedCost", Math.round(suggestedQuantity * (product.getPurchasePrice() == null ? 0.0 : product.getPurchasePrice()) * 100) / 100.0);
        item.put("stockRatio", stockRatio);
        item.put("suggestion", suggestedQuantity > 0 ? "Replenish above safety stock as soon as possible" : "Inventory is stable; keep routine checks");
        return item;
    }

    private int safeStock(Product product) {
        return product.getStock() == null ? 0 : product.getStock();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0.");
        }
    }
}
