package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.dto.AddToCartRequest;
import com.precisionpath.lab_service.entity.CartItem;
import com.precisionpath.lab_service.entity.ItemType;
import com.precisionpath.lab_service.entity.LabTest;
import com.precisionpath.lab_service.entity.TestPackage;
import com.precisionpath.lab_service.exception.DuplicateResourceException;
import com.precisionpath.lab_service.repository.CartItemRepository;
import com.precisionpath.lab_service.repository.LabTestRepository;
import com.precisionpath.lab_service.repository.TestPackageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private LabTestRepository labTestRepository;
    @Mock private TestPackageRepository testPackageRepository;

    @InjectMocks
    private CartService cartService;

    private final UUID patientId = UUID.randomUUID();

    private LabTest test(String name, int price) {
        return LabTest.builder().id(UUID.randomUUID()).name(name)
                .price(BigDecimal.valueOf(price)).active(true).build();
    }

    @Test
    void addingTestAlreadyInsideCartPackageIsRejected() {
        LabTest cbc = test("CBC", 300);
        TestPackage pkg = TestPackage.builder().id(UUID.randomUUID()).name("Basic Health")
                .price(BigDecimal.valueOf(500)).tests(new LinkedHashSet<>(Set.of(cbc))).build();
        CartItem existing = CartItem.builder().id(UUID.randomUUID())
                .itemType(ItemType.PACKAGE).testPackage(pkg).build();

        when(cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId))
                .thenReturn(new ArrayList<>(List.of(existing)));
        when(labTestRepository.findByIdAndActiveTrue(cbc.getId())).thenReturn(Optional.of(cbc));

        assertThatThrownBy(() -> cartService.addItem(patientId, new AddToCartRequest(ItemType.TEST, cbc.getId())))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Basic Health");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addingPackageOverlappingCartTestIsRejected() {
        LabTest cbc = test("CBC", 300);
        TestPackage pkg = TestPackage.builder().id(UUID.randomUUID()).name("Basic Health")
                .price(BigDecimal.valueOf(500)).tests(new LinkedHashSet<>(Set.of(cbc, test("LFT", 400)))).build();
        CartItem existing = CartItem.builder().id(UUID.randomUUID())
                .itemType(ItemType.TEST).labTest(cbc).build();

        when(cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId))
                .thenReturn(new ArrayList<>(List.of(existing)));
        when(testPackageRepository.findByIdAndActiveTrue(pkg.getId())).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> cartService.addItem(patientId, new AddToCartRequest(ItemType.PACKAGE, pkg.getId())))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CBC");
    }

    @Test
    void cartTotalSumsItemPrices() {
        LabTest cbc = test("CBC", 300);
        LabTest tsh = test("TSH", 450);

        when(cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId))
                .thenReturn(new ArrayList<>(List.of(
                        CartItem.builder().id(UUID.randomUUID()).itemType(ItemType.TEST).labTest(cbc).build()
                )));
        when(labTestRepository.findByIdAndActiveTrue(tsh.getId())).thenReturn(Optional.of(tsh));

        var cart = cartService.addItem(patientId, new AddToCartRequest(ItemType.TEST, tsh.getId()));

        assertThat(cart.itemCount()).isEqualTo(2);
        assertThat(cart.totalAmount()).isEqualByComparingTo("750");
    }
}
