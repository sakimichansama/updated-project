package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.dto.OrderItemRequest;
import com.kfc.kfc_backend.dto.OrderRequest;
import com.kfc.kfc_backend.entity.*;
import com.kfc.kfc_backend.repository.EmployeeRepository;
import com.kfc.kfc_backend.repository.OrderItemRepository;
import com.kfc.kfc_backend.repository.OrderRecordRepository;
import com.kfc.kfc_backend.repository.ProductRepository;
import com.kfc.kfc_backend.repository.WorkHourRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelReportService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private OrderRecordRepository orderRecordRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private SalesService salesService;
    @Autowired
    private ProfitService profitService;
    @Autowired
    private PayrollService payrollService;
    @Autowired
    private WorkHourService workHourService;
    @Autowired
    private WorkHourRepository workHourRepository;
    @Autowired
    private EmployeePerformanceService employeePerformanceService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private WasteService wasteService;

    private final DataFormatter formatter = new DataFormatter();

    public byte[] exportReport(String type, String month, LocalDate start, LocalDate end) {
        try (Workbook workbook = new XSSFWorkbook()) {
            switch (type) {
                case "products" -> exportProducts(workbook);
                case "employees" -> exportEmployees(workbook);
                case "orders" -> exportOrders(workbook, start, end);
                case "in-stock" -> exportInStock(workbook);
                case "out-stock" -> exportOutStock(workbook);
                case "sales" -> exportSales(workbook, start, end);
                case "profit" -> exportProfit(workbook, parseMonth(month));
                case "payroll" -> exportPayroll(workbook, parseMonth(month));
                case "work-hours" -> exportWorkHours(workbook, parseMonth(month));
                case "employee-performance" -> exportEmployeePerformance(workbook, parseMonth(month));
                case "inventory-alerts" -> exportInventoryAlerts(workbook);
                case "waste" -> exportWaste(workbook);
                default -> throw new RuntimeException("Unsupported report type: " + type);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel export failed", e);
        }
    }

    public Map<String, Object> importReport(String type, MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            return switch (type) {
                case "products" -> importProducts(workbook);
                case "employees" -> importEmployees(workbook);
                case "orders" -> importOrders(workbook);
                case "in-stock" -> importInStock(workbook);
                case "out-stock" -> importOutStock(workbook);
                case "sales" -> importSales(workbook);
                case "work-hours" -> importWorkHours(workbook);
                case "waste" -> importWaste(workbook);
                default -> throw new RuntimeException("Unsupported import type: " + type);
            };
        } catch (IOException e) {
            throw new RuntimeException("Excel import failed", e);
        }
    }

    public byte[] exportTemplate(String type) {
        try (Workbook workbook = new XSSFWorkbook()) {
            switch (type) {
                case "products" -> {
                    Sheet sheet = createSheet(workbook, "Product Template", "Name", "Specification", "Category", "Unit", "Purchase Price", "Sale Price", "Stock", "Min Stock");
                    writeRow(sheet, 1, "Zinger Burger", "Spicy Chicken", "Burger", "piece", 2.50, 5.99, 150, 30);
                    autoSize(sheet, 8);
                }
                case "employees" -> {
                    Sheet sheet = createSheet(workbook, "Employee Template", "Name", "Phone", "ID Card", "Hire Date", "Position", "Monthly Salary");
                    writeRow(sheet, 1, "Zhang Wei", "13800138001", "41010119900307663X", "2026-04-01", "Store Manager", 6500);
                    autoSize(sheet, 6);
                }
                case "orders" -> {
                    Sheet sheet = createSheet(workbook, "Order Template", "Order No.", "Order Time", "Customer", "Creator ID", "Creator Name", "Payment Method", "Discount", "Remark", "Product ID", "Quantity", "Sale Price");
                    writeRow(sheet, 1, "ORD202604170001", "2026-04-17 12:30:00", "Walk-in Customer", 1, "Store Manager", "Cash", 0, "", 1, 2, 5.99);
                    writeRow(sheet, 2, "ORD202604170001", "2026-04-17 12:30:00", "Walk-in Customer", 1, "Store Manager", "Cash", 0, "", 5, 1, 2.29);
                    autoSize(sheet, 11);
                }
                case "in-stock" -> {
                    Sheet sheet = createSheet(workbook, "Inbound Template", "Product ID", "Quantity", "Inbound Price", "Supplier");
                    writeRow(sheet, 1, 1, 20, 2.50, "KFC Central Supply");
                    autoSize(sheet, 4);
                }
                case "out-stock" -> {
                    Sheet sheet = createSheet(workbook, "Outbound Template", "Product ID", "Quantity", "Outbound Reason");
                    writeRow(sheet, 1, 1, 5, "Internal Use");
                    autoSize(sheet, 3);
                }
                case "sales" -> {
                    Sheet sheet = createSheet(workbook, "Sales Template", "Date", "Orders", "Sales");
                    writeRow(sheet, 1, "2026-04-17", 100, 500.00);
                    autoSize(sheet, 3);
                }
                case "work-hours" -> {
                    Sheet sheet = createSheet(workbook, "Work Hour Template", "Employee ID", "Date", "Hours");
                    writeRow(sheet, 1, 1, "2026-04-17", 8);
                    autoSize(sheet, 3);
                }
                case "waste" -> {
                    Sheet sheet = createSheet(workbook, "Waste Template", "Product ID", "Quantity", "Waste Reason");
                    writeRow(sheet, 1, 3, 2, "Expired");
                    autoSize(sheet, 3);
                }
                default -> throw new RuntimeException("Unsupported template type: " + type);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel template export failed", e);
        }
    }

    private void exportProducts(Workbook workbook) {
        Sheet sheet = createSheet(workbook, "Products", "ID", "Name", "Specification", "Category", "Unit", "Purchase Price", "Sale Price", "Stock", "Min Stock");
        int rowIndex = 1;
        for (Product product : productRepository.findAll()) {
            writeRow(sheet, rowIndex++, product.getId(), product.getName(), product.getSpecification(), product.getCategory(),
                    product.getUnit(), product.getPurchasePrice(), product.getSalePrice(), product.getStock(), product.getMinStock());
        }
        autoSize(sheet, 9);
    }

    private void exportEmployees(Workbook workbook) {
        Sheet sheet = createSheet(workbook, "Employees", "ID", "Name", "Phone", "ID Card", "Hire Date", "Position", "Monthly Salary");
        int rowIndex = 1;
        for (Employee employee : employeeRepository.findAll()) {
            writeRow(sheet, rowIndex++, employee.getId(), employee.getName(), employee.getPhone(), employee.getIdCard(),
                    employee.getHireDate(), employee.getPosition(), employee.getMonthlySalary());
        }
        autoSize(sheet, 7);
    }

    private void exportOrders(Workbook workbook, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            end = LocalDate.now();
            start = end.minusDays(29);
        }
        Sheet sheet = createSheet(workbook, "Orders", "Order ID", "Order No.", "Order Time", "Customer", "Creator ID", "Creator Name", "Payment Method", "Order Amount", "Discount", "Status", "Remark", "Product ID", "Product", "Quantity", "Sale Price", "Line Amount");
        int rowIndex = 1;
        for (OrderRecord order : orderRecordRepository.findByOrderTimeBetweenOrderByOrderTimeDesc(start.atStartOfDay(), end.atTime(23, 59, 59))) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            if (items.isEmpty()) {
                writeRow(sheet, rowIndex++, order.getId(), order.getOrderNo(), order.getOrderTime(), order.getCustomerName(), order.getCreatorId(), order.getCreatorName(), order.getPaymentMethod(),
                        order.getOrderAmount(), order.getDiscountAmount(), order.getStatus(), order.getRemark(), "", "", "", "", "");
            }
            for (OrderItem item : items) {
                writeRow(sheet, rowIndex++, order.getId(), order.getOrderNo(), order.getOrderTime(), order.getCustomerName(), order.getCreatorId(), order.getCreatorName(), order.getPaymentMethod(),
                        order.getOrderAmount(), order.getDiscountAmount(), order.getStatus(), order.getRemark(), item.getProductId(),
                        item.getProductName(), item.getQuantity(), item.getSalePrice(), item.getAmount());
            }
        }
        autoSize(sheet, 16);
    }

    private void exportInStock(Workbook workbook) {
        Sheet sheet = createSheet(workbook, "Inbound", "ID", "Product ID", "Quantity", "Inbound Price", "Supplier", "Time");
        int rowIndex = 1;
        for (InStockRecord record : inventoryService.getInRecords()) {
            writeRow(sheet, rowIndex++, record.getId(), record.getProductId(), record.getQuantity(), record.getPrice(), record.getSupplier(), record.getCreateTime());
        }
        autoSize(sheet, 6);
    }

    private void exportOutStock(Workbook workbook) {
        Sheet sheet = createSheet(workbook, "Outbound", "ID", "Order ID", "Waste ID", "Product ID", "Quantity", "Reason", "Cost Price", "Time");
        int rowIndex = 1;
        for (OutStockRecord record : inventoryService.getOutRecords()) {
            writeRow(sheet, rowIndex++, record.getId(), record.getOrderId(), record.getWasteId(), record.getProductId(), record.getQuantity(), record.getReason(), record.getCostPrice(), record.getCreateTime());
        }
        autoSize(sheet, 8);
    }

    private void exportSales(Workbook workbook, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            end = LocalDate.now();
            start = end.minusDays(29);
        }
        Sheet sheet = createSheet(workbook, "Sales", "Date", "Orders", "Sales");
        int rowIndex = 1;
        for (SalesDaily sales : salesService.findByDateRange(start, end)) {
            writeRow(sheet, rowIndex++, sales.getSaleDate(), sales.getOrderCount(), sales.getTotalAmount());
        }
        autoSize(sheet, 3);
    }

    private void exportProfit(Workbook workbook, YearMonth month) {
        Map<String, Object> report = profitService.getMonthlyProfitReport(month.getYear(), month.getMonthValue());
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        Sheet summarySheet = createSheet(workbook, "Profit Summary", "Metric", "Amount / Ratio");
        writeRow(summarySheet, 1, "Sales", summary.get("totalSales"));
        writeRow(summarySheet, 2, "Material Cost", summary.get("materialCost"));
        writeRow(summarySheet, 3, "Labor Cost", summary.get("laborCost"));
        writeRow(summarySheet, 4, "Waste Cost", summary.get("wasteCost"));
        writeRow(summarySheet, 5, "Gross Profit", summary.get("grossProfit"));
        writeRow(summarySheet, 6, "Net Profit", summary.get("netProfit"));
        writeRow(summarySheet, 7, "Net Margin", summary.get("profitMargin"));
        autoSize(summarySheet, 2);

        Sheet dailySheet = createSheet(workbook, "Daily Profit", "Date", "Profit");
        List<Map<String, Object>> dailyList = (List<Map<String, Object>>) report.get("dailyList");
        int rowIndex = 1;
        for (Map<String, Object> row : dailyList) {
            writeRow(dailySheet, rowIndex++, row.get("date"), row.get("profit"));
        }
        autoSize(dailySheet, 2);
    }

    private void exportPayroll(Workbook workbook, YearMonth month) {
        Sheet sheet = createSheet(workbook, "Payroll", "Employee", "Total Hours", "Base Salary", "Gross Pay");
        int rowIndex = 1;
        for (Map<String, Object> row : payrollService.getMonthlyPayroll(month.getYear(), month.getMonthValue())) {
            writeRow(sheet, rowIndex++, row.get("employeeName"), row.get("totalHours"), row.get("baseSalary"),
                    row.get("totalSalary"));
        }
        autoSize(sheet, 4);
    }

    private void exportWorkHours(Workbook workbook, YearMonth month) {
        Map<Long, Employee> employeeMap = new HashMap<>();
        for (Employee employee : employeeRepository.findAll()) {
            employeeMap.put(employee.getId(), employee);
        }
        Sheet sheet = createSheet(workbook, "Work Hours", "ID", "Employee ID", "Employee", "Date", "Hours");
        int rowIndex = 1;
        for (WorkHour workHour : workHourRepository.findByWorkDateBetweenOrderByWorkDateDesc(month.atDay(1), month.atEndOfMonth())) {
            Employee employee = employeeMap.get(workHour.getEmployeeId());
            writeRow(sheet, rowIndex++, workHour.getId(), workHour.getEmployeeId(),
                    employee == null ? "" : employee.getName(), workHour.getWorkDate(), workHour.getHours());
        }
        autoSize(sheet, 5);
    }

    private void exportEmployeePerformance(Workbook workbook, YearMonth month) {
        Map<String, Object> report = employeePerformanceService.getMonthlyPerformance(month.getYear(), month.getMonthValue());
        List<Map<String, Object>> rows = (List<Map<String, Object>>) report.get("list");
        Sheet sheet = createSheet(workbook, "Employee Performance", "Rank", "Employee", "Position", "Total Hours", "Attendance Rate", "Salary", "Cost per Hour", "Score", "Advice");
        int rowIndex = 1;
        for (Map<String, Object> row : rows) {
            writeRow(sheet, rowIndex++, row.get("rank"), row.get("employeeName"), row.get("position"),
                    row.get("totalHours"), row.get("attendanceRate"), row.get("totalSalary"),
                    row.get("salaryPerHour"), row.get("score"), row.get("suggestion"));
        }
        autoSize(sheet, 9);
    }

    private void exportInventoryAlerts(Workbook workbook) {
        Sheet sheet = createSheet(workbook, "Inventory Alerts", "Product", "Category", "Current Stock", "Min Stock", "Shortage", "Suggested Qty", "Estimated Cost", "Advice");
        int rowIndex = 1;
        for (Map<String, Object> row : inventoryService.getReplenishmentSuggestions()) {
            writeRow(sheet, rowIndex++, row.get("productName"), row.get("category"), row.get("stock"), row.get("minStock"),
                    row.get("shortage"), row.get("suggestedQuantity"), row.get("estimatedCost"), row.get("suggestion"));
        }
        autoSize(sheet, 8);
    }

    private void exportWaste(Workbook workbook) {
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : productRepository.findAll()) {
            productMap.put(product.getId(), product);
        }
        Sheet sheet = createSheet(workbook, "Waste", "ID", "Product", "Quantity", "Reason", "Unit Cost", "Status", "Handler", "Action Taken", "Created At", "Closed At");
        int rowIndex = 1;
        for (WasteRecord record : wasteService.findAll()) {
            Product product = productMap.get(record.getProductId());
            writeRow(sheet, rowIndex++, record.getId(), product == null ? record.getProductId() : product.getName(),
                    record.getQuantity(), record.getReason(), record.getUnitCost(), record.getStatus(),
                    record.getHandler(), record.getAction(), record.getCreateTime(), record.getClosedTime());
        }
        autoSize(sheet, 10);
    }

    private Map<String, Object> importProducts(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        int offset = hasIdColumn(sheet) ? 1 : 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            Product product = resolveImportedProduct(row, offset);
            product.setName(readString(row, offset));
            product.setSpecification(readString(row, offset + 1));
            product.setCategory(readString(row, offset + 2));
            product.setUnit(readString(row, offset + 3));
            product.setPurchasePrice(readDouble(row, offset + 4));
            product.setSalePrice(readDouble(row, offset + 5));
            product.setStock(readInt(row, offset + 6));
            product.setMinStock(readInt(row, offset + 7));
            productService.save(product);
            count++;
        }
        return Map.of("imported", count, "type", "products");
    }

    private Map<String, Object> importEmployees(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        int offset = hasIdColumn(sheet) ? 1 : 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            Employee employee = resolveImportedEmployee(row, offset);
            employee.setName(readString(row, offset));
            employee.setPhone(readString(row, offset + 1));
            employee.setIdCard(readString(row, offset + 2));
            employee.setHireDate(readDate(row, offset + 3));
            employee.setPosition(readString(row, offset + 4));
            employee.setMonthlySalary(readDouble(row, offset + 5));
            employeeService.save(employee);
            count++;
        }
        return Map.of("imported", count, "type", "employees");
    }

    private Map<String, Object> importOrders(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        boolean hasCreatorColumns = sheet.getRow(0) != null && readString(sheet.getRow(0), 3).contains("Creator");
        Map<String, OrderRequest> orderMap = new LinkedHashMap<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            String orderNo = readString(row, 0);
            if (orderNo.isBlank()) {
                orderNo = "IMPORT-" + i + "-" + System.currentTimeMillis();
            }
            OrderRequest request = orderMap.computeIfAbsent(orderNo, key -> {
                OrderRequest created = new OrderRequest();
                created.setOrderNo(key);
                created.setOrderTime(readDateTime(row, 1));
                created.setCustomerName(readString(row, 2));
                if (hasCreatorColumns) {
                    created.setCreatorId(readLong(row, 3));
                    created.setCreatorName(readString(row, 4));
                    created.setPaymentMethod(readString(row, 5));
                    created.setDiscountAmount(readDouble(row, 6));
                    created.setRemark(readString(row, 7));
                } else {
                    created.setPaymentMethod(readString(row, 3));
                    created.setDiscountAmount(readDouble(row, 4));
                    created.setRemark(readString(row, 5));
                }
                created.setItems(new ArrayList<>());
                return created;
            });
            int itemOffset = hasCreatorColumns ? 8 : 6;
            OrderItemRequest item = new OrderItemRequest();
            item.setProductId(readLong(row, itemOffset));
            item.setQuantity(readInt(row, itemOffset + 1));
            item.setSalePrice(readDouble(row, itemOffset + 2));
            request.getItems().add(item);
        }
        int count = 0;
        for (OrderRequest request : orderMap.values()) {
            orderService.create(request);
            count++;
        }
        return Map.of("imported", count, "type", "orders");
    }

    private Map<String, Object> importInStock(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            inventoryService.inStock(readLong(row, 0), readInt(row, 1), readDouble(row, 2), readString(row, 3));
            count++;
        }
        return Map.of("imported", count, "type", "in-stock");
    }

    private Map<String, Object> importOutStock(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            inventoryService.outStock(readLong(row, 0), readInt(row, 1), readString(row, 2));
            count++;
        }
        return Map.of("imported", count, "type", "out-stock");
    }

    private Map<String, Object> importSales(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            SalesDaily sales = new SalesDaily();
            sales.setSaleDate(readDate(row, 0));
            sales.setOrderCount(readInt(row, 1));
            sales.setTotalAmount(readDouble(row, 2));
            salesService.save(sales);
            count++;
        }
        return Map.of("imported", count, "type", "sales");
    }

    private Map<String, Object> importWorkHours(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            workHourService.addWorkHour(readLong(row, 0), readDate(row, 1), readDouble(row, 2));
            count++;
        }
        return Map.of("imported", count, "type", "work-hours");
    }

    private Map<String, Object> importWaste(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            wasteService.addWaste(readLong(row, 0), readInt(row, 1), readString(row, 2));
            count++;
        }
        return Map.of("imported", count, "type", "waste");
    }

    private Sheet createSheet(Workbook workbook, String name, String... headers) {
        Sheet sheet = workbook.createSheet(name);
        writeRow(sheet, 0, (Object[]) headers);
        return sheet;
    }

    private void writeRow(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object value = values[i];
            if (value == null) {
                cell.setBlank();
            } else if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        }
    }

    private void autoSize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        return YearMonth.parse(month);
    }

    private boolean isBlank(Row row) {
        if (row == null || row.getPhysicalNumberOfCells() == 0) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            if (!readString(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasIdColumn(Sheet sheet) {
        Row header = sheet.getRow(0);
        return header != null && "id".equalsIgnoreCase(readString(header, 0));
    }

    private Product resolveImportedProduct(Row row, int offset) {
        if (offset == 0 || readString(row, 0).isBlank()) {
            return new Product();
        }
        Long id = readDouble(row, 0).longValue();
        return productRepository.findById(id).orElseGet(() -> {
            Product product = new Product();
            product.setId(id);
            return product;
        });
    }

    private Employee resolveImportedEmployee(Row row, int offset) {
        if (offset == 0 || readString(row, 0).isBlank()) {
            return new Employee();
        }
        Long id = readDouble(row, 0).longValue();
        return employeeRepository.findById(id).orElseGet(() -> {
            Employee employee = new Employee();
            employee.setId(id);
            return employee;
        });
    }

    private String readString(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private Double readDouble(Row row, int index) {
        String value = readString(row, index);
        if (value.isBlank()) {
            return 0.0;
        }
        return Double.parseDouble(value.replace(",", ""));
    }

    private Integer readInt(Row row, int index) {
        return readDouble(row, index).intValue();
    }

    private Long readLong(Row row, int index) {
        return readDouble(row, index).longValue();
    }

    private LocalDate readDate(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell != null && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return LocalDate.parse(readString(row, index));
    }

    private LocalDateTime readDateTime(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell != null && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }
        String value = readString(row, index);
        if (value.isBlank()) {
            return LocalDateTime.now();
        }
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }
}
