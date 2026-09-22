package com.precisionpath.lab_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lab_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Average time a patient spends at the counter for this test. Used by the queue service. */
    @Column(nullable = false)
    private Integer durationMinutes;

    private String sampleType;

    /** Hours after sample collection until the report is ready. */
    private Integer reportTurnaroundHours;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "lab_test_prerequisites",
            joinColumns = @JoinColumn(name = "lab_test_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "prerequisite", nullable = false, length = 500)
    private List<String> prerequisites = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
