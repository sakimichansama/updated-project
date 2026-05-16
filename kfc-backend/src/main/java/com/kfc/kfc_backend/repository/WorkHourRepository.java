package com.kfc.kfc_backend.repository;

import com.kfc.kfc_backend.entity.WorkHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkHourRepository extends JpaRepository<WorkHour, Long> {
    List<WorkHour> findByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate start, LocalDate end);
    List<WorkHour> findByWorkDateBetweenOrderByWorkDateDesc(LocalDate start, LocalDate end);

    @Query("SELECT SUM(w.hours) FROM WorkHour w WHERE w.employeeId = :employeeId AND w.workDate BETWEEN :start AND :end")
    Double sumHoursByEmployeeAndMonth(@Param("employeeId") Long employeeId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
