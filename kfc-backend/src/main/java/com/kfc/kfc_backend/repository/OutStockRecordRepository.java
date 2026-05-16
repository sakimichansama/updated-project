package com.kfc.kfc_backend.repository;

import com.kfc.kfc_backend.entity.OutStockRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutStockRecordRepository extends JpaRepository<OutStockRecord, Long> {
    List<OutStockRecord> findAllByOrderByCreateTimeDesc();
    List<OutStockRecord> findByOrderId(Long orderId);
    List<OutStockRecord> findByWasteId(Long wasteId);
    void deleteByOrderId(Long orderId);
    void deleteByWasteId(Long wasteId);

    @Query("SELECT o FROM OutStockRecord o WHERE o.reason = 'Sales' AND o.createTime BETWEEN :start AND :end")
    List<OutStockRecord> findSalesOutStockBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
