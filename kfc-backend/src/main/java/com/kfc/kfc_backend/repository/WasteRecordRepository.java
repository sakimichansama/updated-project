package com.kfc.kfc_backend.repository;

import com.kfc.kfc_backend.entity.WasteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WasteRecordRepository extends JpaRepository<WasteRecord, Long> {
    List<WasteRecord> findAllByOrderByCreateTimeDesc();

    @Query("SELECT SUM(w.quantity * COALESCE(w.unitCost, p.purchasePrice)) FROM WasteRecord w JOIN Product p ON w.productId = p.id WHERE w.createTime BETWEEN :start AND :end")
    Double sumWasteCostBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
