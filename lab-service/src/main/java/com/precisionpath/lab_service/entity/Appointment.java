package com.precisionpath.lab_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "appointments",
        indexes = {
                @Index(name = "idx_appointment_patient", columnList = "patientId"),
                @Index(name = "idx_appointment_date_status", columnList = "appointmentDate,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    // Patient details copied from the user-service when the appointment is booked,
    // so receipts do not depend on the user-service being reachable later.
    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private String patientEmail;

    @Column(nullable = false, length = 15)
    private String patientPhone;

    private Integer patientAge;

    private String patientGender;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private String prescriptionFileName;

    private String prescriptionFilePath;

    private String prescriptionContentType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String rejectionReason;

    private UUID reviewedBy;

    private LocalDateTime reviewedAt;

    /** Set by the queue-service once a token is issued for the visit. */
    private Integer tokenNumber;

    private Integer estimatedWaitMinutes;

    @Builder.Default
    @OneToMany(
            mappedBy = "appointment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("itemName ASC")
    private List<AppointmentItem> items = new ArrayList<>();

    /** Guards against two staff members approving/rejecting the same request at once. */
    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(AppointmentItem item) {
        item.setAppointment(this);
        items.add(item);
    }

    public boolean hasPrescription() {
        return prescriptionFilePath != null;
    }
}
