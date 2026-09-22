package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.dto.ReportResponse;
import com.precisionpath.lab_service.entity.Appointment;
import com.precisionpath.lab_service.entity.AppointmentStatus;
import com.precisionpath.lab_service.entity.LabTest;
import com.precisionpath.lab_service.entity.Report;
import com.precisionpath.lab_service.exception.InvalidStateException;
import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import com.precisionpath.lab_service.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<AppointmentStatus> REPORTABLE_STATUSES =
            EnumSet.of(AppointmentStatus.APPROVED, AppointmentStatus.COMPLETED);

    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    public record ReportFile(Resource resource, String fileName, String contentType) {
    }

    @Transactional
    public ReportResponse upload(
            UUID staffId,
            Appointment appointment,
            MultipartFile file,
            UUID labTestId,
            String remarks
    ) {

        if (!REPORTABLE_STATUSES.contains(appointment.getStatus())) {
            throw new InvalidStateException(
                    "Reports can be uploaded only for approved or completed appointments"
            );
        }

        LabTest labTest = null;

        if (labTestId != null) {
            labTest = AppointmentService.testsOf(appointment).stream()
                    .filter(test -> test.getId().equals(labTestId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "That test is not part of this appointment"
                    ));
        }

        if (remarks != null && remarks.length() > 1000) {
            throw new IllegalArgumentException("Remarks must not exceed 1000 characters");
        }

        FileStorageService.StoredFile stored = fileStorageService.store(
                "reports", file, FileStorageService.DOCUMENT_TYPES
        );

        Report report = Report.builder()
                .appointment(appointment)
                .labTest(labTest)
                .fileName(stored.originalName())
                .filePath(stored.path())
                .contentType(stored.contentType())
                .fileSize(stored.size())
                .remarks(StringUtils.hasText(remarks) ? remarks.trim() : null)
                .uploadedBy(staffId)
                .build();

        try {
            return ReportResponse.from(reportRepository.save(report));
        } catch (RuntimeException exception) {
            fileStorageService.delete(stored.path());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReports(UUID appointmentId) {

        return reportRepository.findAllByAppointmentIdOrderByUploadedAtAsc(appointmentId).stream()
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportFile downloadForPatient(UUID patientId, UUID reportId) {

        return toFile(reportRepository.findByIdAndAppointmentPatientId(reportId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found")));
    }

    @Transactional(readOnly = true)
    public ReportFile download(UUID reportId) {

        return toFile(findReport(reportId));
    }

    @Transactional
    public void delete(UUID reportId) {

        Report report = findReport(reportId);

        reportRepository.delete(report);
        fileStorageService.delete(report.getFilePath());
    }

    private Report findReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private ReportFile toFile(Report report) {
        return new ReportFile(
                fileStorageService.load(report.getFilePath()),
                report.getFileName(),
                report.getContentType()
        );
    }
}
