package com.kfc.kfc_backend.repository;

import com.kfc.kfc_backend.entity.SalesDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesDailyRepository extends JpaRepository<SalesDaily, Long> {
    List<SalesDaily> findBySaleDateBetweenOrderBySaleDateAsc(LocalDate start, LocalDate end);
    Optional<SalesDaily> findBySaleDate(LocalDate saleDate);

    @Query("SELECT SUM(s.totalAmount) FROM SalesDaily s WHERE s.saleDate BETWEEN :start AND :end")
    Double sumTotalAmountBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
