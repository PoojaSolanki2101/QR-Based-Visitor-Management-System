package com.visitor.management.controller;

import com.google.zxing.WriterException;
import com.visitor.management.entity.Visitor;
import com.visitor.management.repository.VisitorRepository;
import com.visitor.management.service.QRCodeService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;


@Controller
public class VisitorController {

    private final VisitorRepository visitorRepository;
    private final QRCodeService qrCodeService;

    // Constructor
    public VisitorController(
            VisitorRepository visitorRepository,
            QRCodeService qrCodeService) {

        this.visitorRepository = visitorRepository;
        this.qrCodeService = qrCodeService;
    }

    // Show Registration Form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {

        model.addAttribute("visitor", new Visitor());

        return "register";
    }

    // Register Visitor
    @PostMapping("/register")
    public String registerVisitor(@ModelAttribute Visitor visitor) {

        visitor.setCheckInTime(LocalDateTime.now());
        visitor.setStatus("INSIDE");
        visitor.setCheckOutTime(null);

        visitorRepository.save(visitor);

        return "redirect:/register?success";
    }

    // Show Visitors with QR Codes and Dashboard Statistics
    @GetMapping("/visitors")
    public String showVisitors(Model model)
            throws WriterException, IOException {

        List<Visitor> visitors = visitorRepository.findAll();

        // Generate QR Codes
        for (Visitor visitor : visitors) {

            String qrCode = qrCodeService.generateQRCode(
                    "VISITOR_ID:" + visitor.getId()
            );

            visitor.setQrCode(qrCode);
        }

        // Total Visitors
        long totalVisitors = visitors.size();
        // Today's Visitors
       LocalDate today = LocalDate.now();

long todayVisitors = visitors.stream()
        .filter(v -> v.getCheckInTime() != null)
        .filter(v -> v.getCheckInTime().toLocalDate().equals(today))
        .count();

        // Currently Inside
        long insideVisitors = visitors.stream()
                .filter(v -> "INSIDE".equals(v.getStatus()))
                .count();

        // Checked-Out Visitors
        long checkedOutVisitors = visitors.stream()
                .filter(v -> "CHECKED_OUT".equals(v.getStatus()))
                .count();

        // Send Data to Dashboard
        model.addAttribute("visitors", visitors);
        model.addAttribute("totalVisitors", totalVisitors);
        model.addAttribute("insideVisitors", insideVisitors);
        model.addAttribute("checkedOutVisitors", checkedOutVisitors);
        model.addAttribute("todayVisitors", todayVisitors);

        return "visitors";
    }

    // Visitor Check-In
    @GetMapping("/checkin/{id}")
    public String checkInVisitor(@PathVariable Long id) {

        Visitor visitor = visitorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid visitor ID: " + id
                        ));

        visitor.setCheckInTime(LocalDateTime.now());
        visitor.setCheckOutTime(null);
        visitor.setStatus("INSIDE");

        visitorRepository.save(visitor);

        return "redirect:/visitors";
    }

    // Visitor Check-Out
    @GetMapping("/checkout/{id}")
    public String checkOutVisitor(@PathVariable Long id) {

        Visitor visitor = visitorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid visitor ID: " + id
                        ));

        visitor.setCheckOutTime(LocalDateTime.now());
        visitor.setStatus("CHECKED_OUT");

        visitorRepository.save(visitor);

        return "redirect:/visitors";
    }

    // Show QR Scanner Page
    @GetMapping("/scan")
    public String showScanner() {

        return "scan";
    }

    // QR Scan: Check-In / Check-Out
    @GetMapping("/scan-visitor/{id}")
    public String scanVisitor(@PathVariable Long id) {

        Visitor visitor = visitorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid visitor ID: " + id
                        ));

        if ("INSIDE".equals(visitor.getStatus())) {

            visitor.setCheckOutTime(LocalDateTime.now());
            visitor.setStatus("CHECKED_OUT");

        } else {

            visitor.setCheckInTime(LocalDateTime.now());
            visitor.setCheckOutTime(null);
            visitor.setStatus("INSIDE");
        }

        visitorRepository.save(visitor);

        return "redirect:/visitors";
    }
    // Search Visitor by Name
@GetMapping("/search")
public String searchVisitor(
        @RequestParam("name") String name,
        Model model) throws WriterException, IOException {

    List<Visitor> visitors =
            visitorRepository.findByNameContainingIgnoreCase(name);

    for (Visitor visitor : visitors) {
        String qrCode = qrCodeService.generateQRCode(
                "VISITOR_ID:" + visitor.getId()
        );
        visitor.setQrCode(qrCode);
    }

    model.addAttribute("visitors", visitors);
    model.addAttribute("totalVisitors", visitors.size());

    model.addAttribute("insideVisitors",
            visitors.stream()
                    .filter(v -> "INSIDE".equals(v.getStatus()))
                    .count());

    model.addAttribute("checkedOutVisitors",
            visitors.stream()
                    .filter(v -> "CHECKED_OUT".equals(v.getStatus()))
                    .count());

    model.addAttribute("searchName", name);

    return "visitors";
}

// Visitor History
@GetMapping("/history")
public String showVisitorHistory(Model model) {

    List<Visitor> visitors = visitorRepository.findAll();

    model.addAttribute("visitors", visitors);

    return "history";
}


// Filter Visitors by Check-In Date
@GetMapping("/history/filter")
public String filterVisitorHistory(
        @RequestParam("date") String date,
        Model model) {

    LocalDate selectedDate = LocalDate.parse(date);

    LocalDateTime start = selectedDate.atStartOfDay();
    LocalDateTime end = selectedDate.plusDays(1).atStartOfDay();

    List<Visitor> visitors =
            visitorRepository.findByCheckInTimeBetween(
                    start, end.minusNanos(1));

    model.addAttribute("visitors", visitors);
    model.addAttribute("selectedDate", date);

    return "history";
}

// Export Visitors to Excel
@GetMapping("/export/excel")
public ResponseEntity<byte[]> exportVisitorsToExcel() throws IOException {

    List<Visitor> visitors = visitorRepository.findAll();

    try (XSSFWorkbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

        Sheet sheet = workbook.createSheet("Visitors");

        // Header Row
        Row headerRow = sheet.createRow(0);

        String[] columns = {
                "ID",
                "Name",
                "Mobile",
                "Email",
                "Person To Meet",
                "Purpose",
                "Check-In Time",
                "Check-Out Time",
                "Status"
        };

        for (int i = 0; i < columns.length; i++) {
            headerRow.createCell(i).setCellValue(columns[i]);
        }

        // Visitor Data
        int rowNum = 1;

        for (Visitor visitor : visitors) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(
                    visitor.getId() == null ? 0 : visitor.getId());

            row.createCell(1).setCellValue(
                    visitor.getName() == null ? "" : visitor.getName());

            row.createCell(2).setCellValue(
                    visitor.getMobile() == null ? "" : visitor.getMobile());

            row.createCell(3).setCellValue(
                    visitor.getEmail() == null ? "" : visitor.getEmail());

            row.createCell(4).setCellValue(
                    visitor.getPersonToMeet() == null ? "" : visitor.getPersonToMeet());

            row.createCell(5).setCellValue(
                    visitor.getPurpose() == null ? "" : visitor.getPurpose());

            row.createCell(6).setCellValue(
                    visitor.getCheckInTime() == null
                            ? ""
                            : visitor.getCheckInTime().toString());

            row.createCell(7).setCellValue(
                    visitor.getCheckOutTime() == null
                            ? ""
                            : visitor.getCheckOutTime().toString());

            row.createCell(8).setCellValue(
                    visitor.getStatus() == null ? "" : visitor.getStatus());
        }

        // Adjust column widths
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(outputStream);

        byte[] excelFile = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        headers.setContentDispositionFormData(
                "attachment", "Visitor_Report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelFile);
    }



}


@GetMapping("/login")
public String showLoginPage() {
    return "login";
}
}