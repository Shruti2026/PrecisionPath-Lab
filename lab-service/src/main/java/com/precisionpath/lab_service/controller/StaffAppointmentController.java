package com.precisionpath.lab_service.controller;

import com.precisionpath.lab_service.dto.*;
import com.precisionpath.lab_service.entity.Appointment;
import com.precisionpath.lab_service.entity.AppointmentStatus;
import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import com.precisionpath.lab_service.security.AuthenticatedUser;
import com.precisionpath.lab_service.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Receptionist / admin operations on appointments.
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffAppointmentController {

    private final AppointmentService appointmentService;
    private final PaymentService paymentService;
    private final ReceiptService receiptService;
    private final ReportService reportService;
    private final FileStorageService fileStorageService;

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status
    ) {

        return ResponseEntity.ok(appointmentService.search(date, status));
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable UUID appointmentId) {

        return ResponseEntity.ok(appointmentService.getAppointment(appointmentId));
    }

    @PatchMapping("/appointments/{appointmentId}/approve")
    public ResponseEntity<AppointmentResponse> approve(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        return ResponseEntity.ok(appointmentService.approve(currentUser.userId(), appointmentId));
    }

    @PatchMapping("/appointments/{appointmentId}/reject")
    public ResponseEntity<AppointmentResponse> reject(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody RejectAppointmentRequest request
    ) {

        return ResponseEntity.ok(
                appointmentService.reject(currentUser.userId(), appointmentId, request.reason())
        );
    }

    @PatchMapping("/appointments/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID appointmentId) {

        return ResponseEntity.ok(appointmentService.complete(appointmentId));
    }

    @GetMapping("/appointments/{appointmentId}/prescription")
    public ResponseEntity<Resource> getPrescription(@PathVariable UUID appointmentId) {

        Appointment appointment = appointmentService.find(appointmentId);

        if (!appointment.hasPrescription()) {
            throw new ResourceNotFoundException("No prescription was uploaded for this appointment");
        }

        return FileResponses.resource(
                fileStorageService.load(appointment.getPrescriptionFilePath()),
                appointment.getPrescriptionFileName(),
                appointment.getPrescriptionContentType()
        );
    }

    // ---------- Payment & receipt ----------

    /** Records a payment made at the lab counter. */
    @PostMapping("/appointments/{appointmentId}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody PaymentRequest request
    ) {

        Appointment appointment = appointmentService.find(appointmentId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.pay(appointment, currentUser.userId(), request, true));
    }

    @GetMapping("/appointments/{appointmentId}/payments")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID appointmentId) {

        return ResponseEntity.ok(paymentService.getPayment(appointmentId));
    }

    @GetMapping("/appointments/{appointmentId}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable UUID appointmentId) {

        return ResponseEntity.ok(receiptService.buildReceipt(appointmentService.find(appointmentId)));
    }

    @GetMapping("/appointments/{appointmentId}/receipt/pdf")
    public ResponseEntity<byte[]> getReceiptPdf(@PathVariable UUID appointmentId) {

        Appointment appointment = appointmentService.find(appointmentId);

        return FileResponses.file(
                receiptService.buildReceiptPdf(appointment),
                ReceiptService.receiptNumber(appointment) + ".pdf",
                MediaType.APPLICATION_PDF_VALUE,
                false
        );
    }

    // ---------- Reports ----------

    @PostMapping(value = "/appointments/{appointmentId}/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> uploadReport(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId,
            @RequestPart MultipartFile file,
            @RequestParam(required = false) UUID labTestId,
            @RequestParam(required = false) String remarks
    ) {

        Appointment appointment = appointmentService.find(appointmentId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reportService.upload(currentUser.userId(), appointment, file, labTestId, remarks));
    }

    @GetMapping("/appointments/{appointmentId}/reports")
    public ResponseEntity<List<ReportResponse>> getReports(@PathVariable UUID appointmentId) {

        appointmentService.find(appointmentId);

        return ResponseEntity.ok(reportService.getReports(appointmentId));
    }

    @GetMapping("/reports/{reportId}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable UUID reportId) {

        ReportService.ReportFile file = reportService.download(reportId);

        return FileResponses.resource(file.resource(), file.fileName(), file.contentType());
    }

    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<MessageResponse> deleteReport(@PathVariable UUID reportId) {

        reportService.delete(reportId);

        return ResponseEntity.ok(new MessageResponse("Report deleted"));
    }
}
