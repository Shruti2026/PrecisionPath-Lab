package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.LabTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LabTestResponse(
        UUID id,
        String code,
        String name,
        String purpose,
        BigDecimal price,
        Integer durationMinutes,
        String sampleType,
        Integer reportTurnaroundHours,
        List<String> prerequisites,
        boolean active
) {

    public static LabTestResponse from(LabTest test) {
        return new LabTestResponse(
                test.getId(),
                test.getCode(),
                test.getName(),
                test.getPurpose(),
                test.getPrice(),
                test.getDurationMinutes(),
                test.getSampleType(),
                test.getReportTurnaroundHours(),
                List.copyOf(test.getPrerequisites()),
                test.isActive()
        );
    }
}
