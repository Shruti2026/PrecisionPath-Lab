package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.client.UserServiceClient;
import com.precisionpath.lab_service.entity.*;
import com.precisionpath.lab_service.exception.InvalidStateException;
import com.precisionpath.lab_service.repository.AppointmentRepository;
import com.precisionpath.lab_service.repository.CartItemRepository;
import com.precisionpath.lab_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 22);

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private UserServiceClient userServiceClient;

    private AppointmentService service;
    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        service = new AppointmentService(
                appointmentRepository, cartItemRepository, paymentRepository,
                paymentService, fileStorageService, userServiceClient, clock
        );
        ReflectionTestUtils.setField(service, "minReasonWords", 50);
        ReflectionTestUtils.setField(service, "maxDaysInAdvance", 90);
    }

    private static String words(int count) {
        return String.join(" ", java.util.Collections.nCopies(count, "word"));
    }

    @Test
    void countWordsIgnoresExtraWhitespace() {
        assertThat(AppointmentService.countWords("  fever \n and   cough ")).isEqualTo(3);
        assertThat(AppointmentService.countWords(null)).isZero();
        assertThat(AppointmentService.countWords("   ")).isZero();
    }

    @Test
    void bookingRejectsPastDate() {
        assertThatThrownBy(() -> service.book(patientId, "Bearer x", TODAY.minusDays(1), words(60), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void bookingRejectsDateTooFarAhead() {
        assertThatThrownBy(() -> service.book(patientId, "Bearer x", TODAY.plusDays(91), words(60), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("90 days");
    }

    @Test
    void bookingWithoutPrescriptionNeedsFiftyWordReason() {
        assertThatThrownBy(() -> service.book(patientId, "Bearer x", TODAY, words(49), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50 words");

        verifyNoInteractions(cartItemRepository, userServiceClient);
    }

    @Test
    void bookingRejectsEmptyCart() {
        when(cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.book(patientId, "Bearer x", TODAY, words(50), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cart is empty");
    }

    @Test
    void bookingRejectsDeactivatedTestInCart() {
        LabTest inactive = LabTest.builder().id(UUID.randomUUID()).name("CBC")
                .price(BigDecimal.TEN).active(false).build();
        CartItem item = CartItem.builder().itemType(ItemType.TEST).labTest(inactive).build();
        when(cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId)).thenReturn(List.of(item));

        assertThatThrownBy(() -> service.book(patientId, "Bearer x", TODAY, words(50), null))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void approveOnlyWorksOnPendingAppointments() {
        Appointment appointment = Appointment.builder().id(UUID.randomUUID())
                .status(AppointmentStatus.REJECTED).appointmentDate(TODAY).build();
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.approve(UUID.randomUUID(), appointment.getId()))
                .isInstanceOf(InvalidStateException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void completeRequiresPayment() {
        Appointment appointment = Appointment.builder().id(UUID.randomUUID())
                .status(AppointmentStatus.APPROVED).appointmentDate(TODAY).build();
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(appointment.getId()))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Payment");
    }

    @Test
    void testsOfExpandsPackagesAndRemovesDuplicates() {
        LabTest cbc = LabTest.builder().id(UUID.randomUUID()).name("CBC").build();
        LabTest lft = LabTest.builder().id(UUID.randomUUID()).name("LFT").build();
        TestPackage pkg = TestPackage.builder().id(UUID.randomUUID()).name("Basic")
                .tests(new java.util.LinkedHashSet<>(Set.of(cbc, lft))).build();

        Appointment appointment = new Appointment();
        appointment.addItem(AppointmentItem.builder().itemType(ItemType.PACKAGE).testPackage(pkg).build());
        appointment.addItem(AppointmentItem.builder().itemType(ItemType.TEST).labTest(cbc).build());

        assertThat(AppointmentService.testsOf(appointment)).extracting(LabTest::getName)
                .containsExactly("CBC", "LFT");
    }
}
