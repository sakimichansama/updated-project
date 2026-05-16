package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.SalesDaily;
import com.kfc.kfc_backend.repository.SalesDailyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SalesService {
    @Autowired
    private SalesDailyRepository salesDailyRepository;

    public List<SalesDaily> findByDateRange(LocalDate start, LocalDate end) {
        return salesDailyRepository.findBySaleDateBetweenOrderBySaleDateAsc(start, end);
    }

    public SalesDaily save(SalesDaily sales) {
        return salesDailyRepository.save(sales);
    }

    public void deleteById(Long id) {
        salesDailyRepository.deleteById(id);
    }

    public Optional<SalesDaily> findById(Long id) {
        return salesDailyRepository.findById(id);
    }

    public double getTotalSalesBetween(LocalDate start, LocalDate end) {
        Double sum = salesDailyRepository.sumTotalAmountBetween(start, end);
        return sum == null ? 0.0 : sum;
    }
}
