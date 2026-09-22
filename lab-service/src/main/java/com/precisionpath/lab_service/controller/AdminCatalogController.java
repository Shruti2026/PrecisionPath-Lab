package com.precisionpath.lab_service.controller;

import com.precisionpath.lab_service.dto.LabTestRequest;
import com.precisionpath.lab_service.dto.LabTestResponse;
import com.precisionpath.lab_service.dto.TestPackageRequest;
import com.precisionpath.lab_service.dto.TestPackageResponse;
import com.precisionpath.lab_service.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CatalogService catalogService;

    // ---------- Tests ----------

    @GetMapping("/tests")
    public ResponseEntity<List<LabTestResponse>> getAllTests() {

        return ResponseEntity.ok(catalogService.getAllTests());
    }

    @PostMapping("/tests")
    public ResponseEntity<LabTestResponse> createTest(
            @Valid @RequestBody LabTestRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(catalogService.createTest(request));
    }

    @PutMapping("/tests/{testId}")
    public ResponseEntity<LabTestResponse> updateTest(
            @PathVariable UUID testId,
            @Valid @RequestBody LabTestRequest request
    ) {

        return ResponseEntity.ok(catalogService.updateTest(testId, request));
    }

    /** Deactivates the test; it disappears from the catalogue but old bookings keep it. */
    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<LabTestResponse> deactivateTest(@PathVariable UUID testId) {

        return ResponseEntity.ok(catalogService.setTestActive(testId, false));
    }

    @PatchMapping("/tests/{testId}/activate")
    public ResponseEntity<LabTestResponse> activateTest(@PathVariable UUID testId) {

        return ResponseEntity.ok(catalogService.setTestActive(testId, true));
    }

    // ---------- Packages ----------

    @GetMapping("/packages")
    public ResponseEntity<List<TestPackageResponse>> getAllPackages() {

        return ResponseEntity.ok(catalogService.getAllPackages());
    }

    @PostMapping("/packages")
    public ResponseEntity<TestPackageResponse> createPackage(
            @Valid @RequestBody TestPackageRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(catalogService.createPackage(request));
    }

    @PutMapping("/packages/{packageId}")
    public ResponseEntity<TestPackageResponse> updatePackage(
            @PathVariable UUID packageId,
            @Valid @RequestBody TestPackageRequest request
    ) {

        return ResponseEntity.ok(catalogService.updatePackage(packageId, request));
    }

    @DeleteMapping("/packages/{packageId}")
    public ResponseEntity<TestPackageResponse> deactivatePackage(@PathVariable UUID packageId) {

        return ResponseEntity.ok(catalogService.setPackageActive(packageId, false));
    }

    @PatchMapping("/packages/{packageId}/activate")
    public ResponseEntity<TestPackageResponse> activatePackage(@PathVariable UUID packageId) {

        return ResponseEntity.ok(catalogService.setPackageActive(packageId, true));
    }
}
