package com.precisionpath.lab_service.controller;

import com.precisionpath.lab_service.dto.AppointmentResponse;
import com.precisionpath.lab_service.dto.InternalAppointmentResponse;
import com.precisionpath.lab_service.dto.QueueInfoRequest;
import com.precisionpath.lab_service.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Service-to-service endpoints for the queue-service. Requires the X-Internal-Api-Key header.
 */
@RestController
@RequestMapping("/api/internal/appointments")
@RequiredArgsConstructor
public class InternalAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/{appointmentId}")
    public ResponseEntity<InternalAppointmentResponse> getAppointment(@PathVariable UUID appointmentId) {

        return ResponseEntity.ok(appointmentService.getForQueue(appointmentId));
    }

    @PutMapping("/{appointmentId}/queue-info")
    public ResponseEntity<AppointmentResponse> updateQueueInfo(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody QueueInfoRequest request
    ) {

        return ResponseEntity.ok(appointmentService.updateQueueInfo(appointmentId, request));
    }
}
