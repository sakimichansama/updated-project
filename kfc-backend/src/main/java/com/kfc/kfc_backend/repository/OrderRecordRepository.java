package com.kfc.kfc_backend.repository;

import com.kfc.kfc_backend.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long> {
    Optional<OrderRecord> findByOrderNo(String orderNo);
    List<OrderRecord> findByOrderTimeBetweenOrderByOrderTimeDesc(LocalDateTime start, LocalDateTime end);
}
