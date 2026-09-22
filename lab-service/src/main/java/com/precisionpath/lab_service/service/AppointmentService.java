package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.client.UserProfile;
import com.precisionpath.lab_service.client.UserServiceClient;
import com.precisionpath.lab_service.dto.AppointmentItemResponse;
import com.precisionpath.lab_service.dto.AppointmentResponse;
import com.precisionpath.lab_service.dto.InternalAppointmentResponse;
import com.precisionpath.lab_service.dto.QueueInfoRequest;
import com.precisionpath.lab_service.entity.*;
import com.precisionpath.lab_service.exception.InvalidStateException;
import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import com.precisionpath.lab_service.repository.AppointmentRepository;
import com.precisionpath.lab_service.repository.CartItemRepository;
import com.precisionpath.lab_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Set<AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.APPROVED);

    private final AppointmentRepository appointmentRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final FileStorageService fileStorageService;
    private final UserServiceClient userServiceClient;
    private final Clock clock;

    @Value("${app.booking.min-reason-words:50}")
    private int minReasonWords;

    @Value("${app.booking.max-days-in-advance:90}")
    private int maxDaysInAdvance;

    // ---------- Patient ----------

    /**
     * Turns the patient's cart into an appointment request. A prescription file
     * or a reason of at least {@code app.booking.min-reason-words} words is required.
     */
    @Transactional
    public AppointmentResponse book(
            UUID patientId,
            String authorizationHeader,
            LocalDate appointmentDate,
            String reason,
            MultipartFile prescription
    ) {

        validateDate(appointmentDate);

        boolean hasPrescription = prescription != null && !prescription.isEmpty();
        String trimmedReason = StringUtils.hasText(reason) ? reason.trim() : null;

        if (!hasPrescription && countWords(trimmedReason) < minReasonWords) {
            throw new IllegalArgumentException(
                    "Upload a prescription or describe the reason for the tests in at least "
                            + minReasonWords + " words"
            );
        }

        List<CartItem> cart = cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId);

        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty. Add tests before booking.");
        }

        cart.forEach(this::ensureStillAvailable);

        if (appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusIn(
                patientId, appointmentDate, ACTIVE_STATUSES)) {
            throw new InvalidStateException(
                    "You already have an appointment request for " + appointmentDate
            );
        }

        UserProfile profile = userServiceClient.getCurrentUser(authorizationHeader);

        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .patientName(profile.fullName())
                .patientEmail(profile.email())
                .patientPhone(profile.phoneNumber())
                .patientAge(profile.age())
                .patientGender(profile.gender())
                .appointmentDate(appointmentDate)
                .status(AppointmentStatus.PENDING)
                .reason(trimmedReason)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart) {

            boolean isTest = cartItem.getItemType() == ItemType.TEST;
            BigDecimal price = isTest
                    ? cartItem.getLabTest().getPrice()
                    : cartItem.getTestPackage().getPrice();

            appointment.addItem(AppointmentItem.builder()
                    .itemType(cartItem.getItemType())
                    .labTest(cartItem.getLabTest())
                    .testPackage(cartItem.getTestPackage())
                    .itemName(isTest ? cartItem.getLabTest().getName() : cartItem.getTestPackage().getName())
                    .price(price)
                    .build());

            total = total.add(price);
        }

        appointment.setTotalAmount(total);

        FileStorageService.StoredFile storedFile = null;

        if (hasPrescription) {
            storedFile = fileStorageService.store(
                    "prescriptions", prescription, FileStorageService.DOCUMENT_TYPES
            );
            appointment.setPrescriptionFileName(storedFile.originalName());
            appointment.setPrescriptionFilePath(storedFile.path());
            appointment.setPrescriptionContentType(storedFile.contentType());
        }

        try {
            Appointment saved = appointmentRepository.save(appointment);
            cartItemRepository.deleteAllByPatientId(patientId);
            return toResponse(saved);
        } catch (RuntimeException exception) {
            if (storedFile != null) {
                fileStorageService.delete(storedFile.path());
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(UUID patientId) {

        return appointmentRepository
                .findAllByPatientIdOrderByAppointmentDateDescCreatedAtDesc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getPatientAppointment(UUID patientId, UUID appointmentId) {

        return toResponse(findOwned(patientId, appointmentId));
    }

    @Transactional
    public AppointmentResponse cancel(UUID patientId, UUID appointmentId) {

        Appointment appointment = findOwned(patientId, appointmentId);

        if (!ACTIVE_STATUSES.contains(appointment.getStatus())) {
            throw new InvalidStateException(
                    "Only pending or approved appointments can be cancelled"
            );
        }

        if (appointment.getAppointmentDate().isBefore(today())) {
            throw new InvalidStateException("Past appointments cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        paymentService.refundIfPaid(appointment);

        return toResponse(appointmentRepository.save(appointment));
    }

    // ---------- Staff ----------

    @Transactional(readOnly = true)
    public List<AppointmentResponse> search(LocalDate date, AppointmentStatus status) {

        return appointmentRepository.search(date, status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID appointmentId) {

        return toResponse(find(appointmentId));
    }

    @Transactional
    public AppointmentResponse approve(UUID staffId, UUID appointmentId) {

        Appointment appointment = find(appointmentId);

        requireStatus(appointment, AppointmentStatus.PENDING, "Only pending appointments can be approved");

        if (appointment.getAppointmentDate().isBefore(today())) {
            throw new InvalidStateException("The appointment date has already passed");
        }

        appointment.setStatus(AppointmentStatus.APPROVED);
        markReviewed(appointment, staffId);

        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse reject(UUID staffId, UUID appointmentId, String reason) {

        Appointment appointment = find(appointmentId);

        requireStatus(appointment, AppointmentStatus.PENDING, "Only pending appointments can be rejected");

        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setRejectionReason(reason.trim());
        markReviewed(appointment, staffId);

        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse complete(UUID appointmentId) {

        Appointment appointment = find(appointmentId);

        requireStatus(appointment, AppointmentStatus.APPROVED, "Only approved appointments can be completed");

        boolean paid = paymentRepository.findByAppointmentId(appointmentId)
                .map(payment -> payment.getStatus() == PaymentStatus.PAID)
                .orElse(false);

        if (!paid) {
            throw new InvalidStateException("Payment must be received before completing the visit");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);

        return toResponse(appointmentRepository.save(appointment));
    }

    // ---------- Internal (queue-service) ----------

    @Transactional(readOnly = true)
    public InternalAppointmentResponse getForQueue(UUID appointmentId) {

        Appointment appointment = find(appointmentId);
        List<LabTest> tests = testsOf(appointment);

        return new InternalAppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getPatientName(),
                appointment.getAppointmentDate(),
                appointment.getStatus(),
                tests.stream()
                        .map(test -> new InternalAppointmentResponse.Test(
                                test.getId(), test.getCode(), test.getName(), test.getDurationMinutes()))
                        .toList(),
                tests.stream().mapToInt(LabTest::getDurationMinutes).sum()
        );
    }

    @Transactional
    public AppointmentResponse updateQueueInfo(UUID appointmentId, QueueInfoRequest request) {

        Appointment appointment = find(appointmentId);

        requireStatus(appointment, AppointmentStatus.APPROVED,
                "Queue details can only be set on approved appointments");

        appointment.setTokenNumber(request.tokenNumber());
        appointment.setEstimatedWaitMinutes(request.estimatedWaitMinutes());

        return toResponse(appointmentRepository.save(appointment));
    }

    // ---------- Shared helpers ----------

    public Appointment find(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    /** Patients only see their own appointments; anyone else's looks like it does not exist. */
    public Appointment findOwned(UUID patientId, UUID appointmentId) {
        return appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    /** Every distinct test in the appointment, with packages expanded, in item order. */
    public static List<LabTest> testsOf(Appointment appointment) {

        Map<UUID, LabTest> tests = new LinkedHashMap<>();

        for (AppointmentItem item : appointment.getItems()) {
            if (item.getItemType() == ItemType.TEST) {
                tests.putIfAbsent(item.getLabTest().getId(), item.getLabTest());
            } else {
                item.getTestPackage().getTests().stream()
                        .sorted(Comparator.comparing(LabTest::getName))
                        .forEach(test -> tests.putIfAbsent(test.getId(), test));
            }
        }

        return List.copyOf(tests.values());
    }

    static int countWords(String text) {

        if (!StringUtils.hasText(text)) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

    private void validateDate(LocalDate date) {

        LocalDate today = today();

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Appointment date cannot be in the past");
        }

        if (date.isAfter(today.plusDays(maxDaysInAdvance))) {
            throw new IllegalArgumentException(
                    "Appointments can be booked at most " + maxDaysInAdvance + " days in advance"
            );
        }
    }

    private void ensureStillAvailable(CartItem item) {

        if (item.getItemType() == ItemType.TEST && !item.getLabTest().isActive()) {
            throw new InvalidStateException(
                    item.getLabTest().getName() + " is no longer available. Remove it from your cart."
            );
        }

        if (item.getItemType() == ItemType.PACKAGE && (!item.getTestPackage().isActive() ||
                item.getTestPackage().getTests().stream().anyMatch(test -> !test.isActive()))) {
            throw new InvalidStateException(
                    item.getTestPackage().getName() + " is no longer available. Remove it from your cart."
            );
        }
    }

    private void requireStatus(Appointment appointment, AppointmentStatus expected, String message) {
        if (appointment.getStatus() != expected) {
            throw new InvalidStateException(message);
        }
    }

    private void markReviewed(Appointment appointment, UUID staffId) {
        appointment.setReviewedBy(staffId);
        appointment.setReviewedAt(LocalDateTime.now(clock));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private AppointmentResponse toResponse(Appointment appointment) {

        PaymentStatus paymentStatus = paymentRepository.findByAppointmentId(appointment.getId())
                .map(Payment::getStatus)
                .orElse(null);

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getPatientName(),
                appointment.getPatientPhone(),
                appointment.getAppointmentDate(),
                appointment.getStatus(),
                appointment.getItems().stream().map(AppointmentItemResponse::from).toList(),
                appointment.getTotalAmount(),
                appointment.getReason(),
                appointment.hasPrescription(),
                appointment.getPrescriptionFileName(),
                appointment.getRejectionReason(),
                paymentStatus,
                appointment.getTokenNumber(),
                appointment.getEstimatedWaitMinutes(),
                appointment.getCreatedAt(),
                appointment.getReviewedAt()
        );
    }
}
