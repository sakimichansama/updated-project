package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.WorkHour;
import com.kfc.kfc_backend.repository.WorkHourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class WorkHourService {
    @Autowired
    private WorkHourRepository workHourRepository;

    public void addWorkHour(Long employeeId, LocalDate workDate, Double hours) {
        WorkHour wh = new WorkHour();
        wh.setEmployeeId(employeeId);
        wh.setWorkDate(workDate);
        wh.setHours(hours);
        workHourRepository.save(wh);
    }

    public Double getTotalHoursByEmployeeAndMonth(Long employeeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return workHourRepository.sumHoursByEmployeeAndMonth(employeeId, start, end);
    }
}
