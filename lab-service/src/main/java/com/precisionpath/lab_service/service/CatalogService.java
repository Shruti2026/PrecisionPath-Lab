package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.dto.LabTestRequest;
import com.precisionpath.lab_service.dto.LabTestResponse;
import com.precisionpath.lab_service.dto.TestPackageRequest;
import com.precisionpath.lab_service.dto.TestPackageResponse;
import com.precisionpath.lab_service.entity.LabTest;
import com.precisionpath.lab_service.entity.TestPackage;
import com.precisionpath.lab_service.exception.DuplicateResourceException;
import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import com.precisionpath.lab_service.repository.LabTestRepository;
import com.precisionpath.lab_service.repository.TestPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tests and packages. Deleting only deactivates, because old appointments,
 * receipts and reports still point at the catalogue entries.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final LabTestRepository labTestRepository;
    private final TestPackageRepository testPackageRepository;

    // ---------- Tests ----------

    @Transactional(readOnly = true)
    public List<LabTestResponse> getActiveTests(String search) {

        List<LabTest> tests = StringUtils.hasText(search)
                ? labTestRepository.findAllByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(search.trim())
                : labTestRepository.findAllByActiveTrueOrderByNameAsc();

        return tests.stream().map(LabTestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LabTestResponse> getAllTests() {

        return labTestRepository.findAllByOrderByNameAsc().stream()
                .map(LabTestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LabTestResponse getActiveTest(UUID id) {

        return LabTestResponse.from(
                labTestRepository.findByIdAndActiveTrue(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Test not found"))
        );
    }

    @Transactional
    public LabTestResponse createTest(LabTestRequest request) {

        String code = normalizeCode(request.code());

        if (labTestRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A test with code " + code + " already exists");
        }

        LabTest test = new LabTest();
        apply(test, request, code);

        return LabTestResponse.from(labTestRepository.save(test));
    }

    @Transactional
    public LabTestResponse updateTest(UUID id, LabTestRequest request) {

        LabTest test = findTest(id);
        String code = normalizeCode(request.code());

        if (labTestRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("A test with code " + code + " already exists");
        }

        apply(test, request, code);

        return LabTestResponse.from(labTestRepository.save(test));
    }

    @Transactional
    public LabTestResponse setTestActive(UUID id, boolean active) {

        LabTest test = findTest(id);
        test.setActive(active);

        return LabTestResponse.from(labTestRepository.save(test));
    }

    // ---------- Packages ----------

    @Transactional(readOnly = true)
    public List<TestPackageResponse> getActivePackages() {

        return testPackageRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(TestPackageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestPackageResponse> getAllPackages() {

        return testPackageRepository.findAllByOrderByNameAsc().stream()
                .map(TestPackageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestPackageResponse getActivePackage(UUID id) {

        return TestPackageResponse.from(
                testPackageRepository.findByIdAndActiveTrue(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Package not found"))
        );
    }

    @Transactional
    public TestPackageResponse createPackage(TestPackageRequest request) {

        String name = request.name().trim();

        if (testPackageRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A package named " + name + " already exists");
        }

        TestPackage testPackage = new TestPackage();
        apply(testPackage, request, name);

        return TestPackageResponse.from(testPackageRepository.save(testPackage));
    }

    @Transactional
    public TestPackageResponse updatePackage(UUID id, TestPackageRequest request) {

        TestPackage testPackage = findPackage(id);
        String name = request.name().trim();

        if (testPackageRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A package named " + name + " already exists");
        }

        apply(testPackage, request, name);

        return TestPackageResponse.from(testPackageRepository.save(testPackage));
    }

    @Transactional
    public TestPackageResponse setPackageActive(UUID id, boolean active) {

        TestPackage testPackage = findPackage(id);

        if (active) {
            requireAllActive(testPackage.getTests());
        }

        testPackage.setActive(active);

        return TestPackageResponse.from(testPackageRepository.save(testPackage));
    }

    // ---------- Helpers ----------

    private void apply(LabTest test, LabTestRequest request, String code) {

        test.setCode(code);
        test.setName(request.name().trim());
        test.setPurpose(request.purpose().trim());
        test.setPrice(request.price());
        test.setDurationMinutes(request.durationMinutes());
        test.setSampleType(StringUtils.hasText(request.sampleType()) ? request.sampleType().trim() : null);
        test.setReportTurnaroundHours(request.reportTurnaroundHours());

        List<String> prerequisites = request.prerequisites() == null
                ? new ArrayList<>()
                : request.prerequisites().stream().map(String::trim).toList();

        test.getPrerequisites().clear();
        test.getPrerequisites().addAll(prerequisites);
    }

    private void apply(TestPackage testPackage, TestPackageRequest request, String name) {

        Set<LabTest> tests = new LinkedHashSet<>(labTestRepository.findAllById(request.testIds()));

        if (tests.size() != request.testIds().size()) {
            throw new ResourceNotFoundException("One or more tests in the package do not exist");
        }

        requireAllActive(tests);

        testPackage.setName(name);
        testPackage.setDescription(request.description());
        testPackage.setPrice(request.price());
        testPackage.getTests().clear();
        testPackage.getTests().addAll(tests);
    }

    private void requireAllActive(Set<LabTest> tests) {

        tests.stream()
                .filter(test -> !test.isActive())
                .findFirst()
                .ifPresent(test -> {
                    throw new IllegalArgumentException(
                            "Test " + test.getName() + " is inactive and cannot be part of an active package"
                    );
                });
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private LabTest findTest(UUID id) {
        return labTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
    }

    private TestPackage findPackage(UUID id) {
        return testPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
    }
}
