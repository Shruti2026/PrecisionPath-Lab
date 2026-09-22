package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.Report;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponse(
        UUID reportId,
        UUID appointmentId,
        UUID labTestId,
        String labTestName,
        String fileName,
        String contentType,
        Long fileSize,
        String remarks,
        LocalDateTime uploadedAt
) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getAppointment().getId(),
                report.getLabTest() == null ? null : report.getLabTest().getId(),
                report.getLabTest() == null ? null : report.getLabTest().getName(),
                report.getFileName(),
                report.getContentType(),
                report.getFileSize(),
                report.getRemarks(),
                report.getUploadedAt()
        );
    }
}
