package com.precisionpath.lab_service.controller;

import com.precisionpath.lab_service.dto.*;
import com.precisionpath.lab_service.entity.Appointment;
import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import com.precisionpath.lab_service.security.AuthenticatedUser;
import com.precisionpath.lab_service.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientAppointmentController {

    private final AppointmentService appointmentService;
    private final PaymentService paymentService;
    private final ReceiptService receiptService;
    private final ReportService reportService;
    private final FileStorageService fileStorageService;

    /**
     * Books the tests in the cart. Send as multipart/form-data with
     * appointmentDate (yyyy-MM-dd), and a prescription file and/or reason.
     */
    @PostMapping(value = "/appointments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppointmentResponse> book(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            @RequestParam(required = false) String reason,
            @RequestPart(required = false) MultipartFile prescription
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appointmentService.book(
                        currentUser.userId(), authorization, appointmentDate, reason, prescription
                ));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        return ResponseEntity.ok(appointmentService.getPatientAppointments(currentUser.userId()));
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        return ResponseEntity.ok(
                appointmentService.getPatientAppointment(currentUser.userId(), appointmentId)
        );
    }

    @PatchMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        return ResponseEntity.ok(appointmentService.cancel(currentUser.userId(), appointmentId));
    }

    @GetMapping("/appointments/{appointmentId}/prescription")
    public ResponseEntity<Resource> getPrescription(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        Appointment appointment = appointmentService.findOwned(currentUser.userId(), appointmentId);

        return prescriptionOf(appointment);
    }

    // ---------- Payment & receipt ----------

    @PostMapping("/appointments/{appointmentId}/payments")
    public ResponseEntity<PaymentResponse> pay(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody PaymentRequest request
    ) {

        Appointment appointment = appointmentService.findOwned(currentUser.userId(), appointmentId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.pay(appointment, currentUser.userId(), request, false));
    }

    @GetMapping("/appointments/{appointmentId}/payments")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        appointmentService.findOwned(currentUser.userId(), appointmentId);

        return ResponseEntity.ok(paymentService.getPayment(appointmentId));
    }

    @GetMapping("/appointments/{appointmentId}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        Appointment appointment = appointmentService.findOwned(currentUser.userId(), appointmentId);

        return ResponseEntity.ok(receiptService.buildReceipt(appointment));
    }

    @GetMapping("/appointments/{appointmentId}/receipt/pdf")
    public ResponseEntity<byte[]> getReceiptPdf(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        Appointment appointment = appointmentService.findOwned(currentUser.userId(), appointmentId);

        return FileResponses.file(
                receiptService.buildReceiptPdf(appointment),
                ReceiptService.receiptNumber(appointment) + ".pdf",
                MediaType.APPLICATION_PDF_VALUE,
                false
        );
    }

    // ---------- Reports ----------

    @GetMapping("/appointments/{appointmentId}/reports")
    public ResponseEntity<List<ReportResponse>> getReports(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID appointmentId
    ) {

        appointmentService.findOwned(currentUser.userId(), appointmentId);

        return ResponseEntity.ok(reportService.getReports(appointmentId));
    }

    @GetMapping("/reports/{reportId}/download")
    public ResponseEntity<Resource> downloadReport(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID reportId
    ) {

        ReportService.ReportFile file = reportService.downloadForPatient(currentUser.userId(), reportId);

        return FileResponses.resource(file.resource(), file.fileName(), file.contentType());
    }

    private ResponseEntity<Resource> prescriptionOf(Appointment appointment) {

        if (!appointment.hasPrescription()) {
            throw new ResourceNotFoundException(
                    "No prescription was uploaded for this appointment"
            );
        }

        return FileResponses.resource(
                fileStorageService.load(appointment.getPrescriptionFilePath()),
                appointment.getPrescriptionFileName(),
                appointment.getPrescriptionContentType()
        );
    }
}
