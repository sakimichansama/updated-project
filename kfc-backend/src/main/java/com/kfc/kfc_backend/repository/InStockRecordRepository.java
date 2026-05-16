package com.kfc.kfc_backend.repository;

import com.kfc.kfc_backend.entity.InStockRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InStockRecordRepository extends JpaRepository<InStockRecord, Long> {
    List<InStockRecord> findAllByOrderByCreateTimeDesc();
}
