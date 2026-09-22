package com.precisionpath.lab_service.controller;

import com.precisionpath.lab_service.dto.LabTestResponse;
import com.precisionpath.lab_service.dto.TestPackageResponse;
import com.precisionpath.lab_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public, read-only catalogue of active tests and packages.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/tests")
    public ResponseEntity<List<LabTestResponse>> getTests(
            @RequestParam(required = false) String search
    ) {

        return ResponseEntity.ok(catalogService.getActiveTests(search));
    }

    @GetMapping("/tests/{testId}")
    public ResponseEntity<LabTestResponse> getTest(@PathVariable UUID testId) {

        return ResponseEntity.ok(catalogService.getActiveTest(testId));
    }

    @GetMapping("/packages")
    public ResponseEntity<List<TestPackageResponse>> getPackages() {

        return ResponseEntity.ok(catalogService.getActivePackages());
    }

    @GetMapping("/packages/{packageId}")
    public ResponseEntity<TestPackageResponse> getPackage(@PathVariable UUID packageId) {

        return ResponseEntity.ok(catalogService.getActivePackage(packageId));
    }
}
