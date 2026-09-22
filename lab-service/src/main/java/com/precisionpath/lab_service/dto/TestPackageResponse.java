package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.LabTest;
import com.precisionpath.lab_service.entity.TestPackage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record TestPackageResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        /** Sum of the individual test prices, so the UI can show the saving. */
        BigDecimal individualTotal,
        List<LabTestResponse> tests,
        boolean active
) {

    public static TestPackageResponse from(TestPackage testPackage) {

        List<LabTestResponse> tests = testPackage.getTests().stream()
                .sorted(Comparator.comparing(LabTest::getName))
                .map(LabTestResponse::from)
                .toList();

        BigDecimal individualTotal = testPackage.getTests().stream()
                .map(LabTest::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TestPackageResponse(
                testPackage.getId(),
                testPackage.getName(),
                testPackage.getDescription(),
                testPackage.getPrice(),
                individualTotal,
                tests,
                testPackage.isActive()
        );
    }
}
