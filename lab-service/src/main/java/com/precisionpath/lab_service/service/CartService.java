package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.dto.AddToCartRequest;
import com.precisionpath.lab_service.dto.CartResponse;
import com.precisionpath.lab_service.entity.CartItem;
import com.precisionpath.lab_service.entity.ItemType;
import com.precisionpath.lab_service.entity.LabTest;
import com.precisionpath.lab_service.entity.TestPackage;
import com.precisionpath.lab_service.exception.DuplicateResourceException;
import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import com.precisionpath.lab_service.repository.CartItemRepository;
import com.precisionpath.lab_service.repository.LabTestRepository;
import com.precisionpath.lab_service.repository.TestPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final LabTestRepository labTestRepository;
    private final TestPackageRepository testPackageRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID patientId) {

        return toResponse(cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId));
    }

    @Transactional
    public CartResponse addItem(UUID patientId, AddToCartRequest request) {

        List<CartItem> cart = cartItemRepository.findAllByPatientIdOrderByAddedAtAsc(patientId);

        CartItem item = CartItem.builder()
                .patientId(patientId)
                .itemType(request.itemType())
                .build();

        if (request.itemType() == ItemType.TEST) {

            LabTest test = labTestRepository.findByIdAndActiveTrue(request.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

            ensureNotAlreadyInCart(cart, Set.of(test));
            item.setLabTest(test);

        } else {

            TestPackage testPackage = testPackageRepository.findByIdAndActiveTrue(request.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

            if (cart.stream().anyMatch(existing ->
                    existing.getTestPackage() != null &&
                            existing.getTestPackage().getId().equals(testPackage.getId()))) {
                throw new DuplicateResourceException("This package is already in your cart");
            }

            ensureNotAlreadyInCart(cart, testPackage.getTests());
            item.setTestPackage(testPackage);
        }

        cartItemRepository.save(item);
        cart.add(item);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID patientId, UUID cartItemId) {

        CartItem item = cartItemRepository.findByIdAndPatientId(cartItemId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItemRepository.delete(item);

        return getCart(patientId);
    }

    @Transactional
    public void clear(UUID patientId) {

        cartItemRepository.deleteAllByPatientId(patientId);
    }

    /**
     * Rejects a test that is already in the cart, either on its own or inside a package,
     * so a patient is never billed twice for the same test.
     */
    private void ensureNotAlreadyInCart(List<CartItem> cart, Collection<LabTest> newTests) {

        for (CartItem existing : cart) {

            Collection<LabTest> existingTests = existing.getItemType() == ItemType.TEST
                    ? List.of(existing.getLabTest())
                    : existing.getTestPackage().getTests();

            for (LabTest existingTest : existingTests) {
                for (LabTest newTest : newTests) {
                    if (existingTest.getId().equals(newTest.getId())) {

                        String where = existing.getItemType() == ItemType.TEST
                                ? "your cart"
                                : "the package " + existing.getTestPackage().getName();

                        throw new DuplicateResourceException(
                                newTest.getName() + " is already included in " + where
                        );
                    }
                }
            }
        }
    }

    static List<LabTest> testsOf(CartItem item) {
        return item.getItemType() == ItemType.TEST
                ? List.of(item.getLabTest())
                : List.copyOf(item.getTestPackage().getTests());
    }

    private CartResponse toResponse(List<CartItem> items) {

        List<CartResponse.CartItemResponse> responses = items.stream()
                .map(item -> item.getItemType() == ItemType.TEST
                        ? new CartResponse.CartItemResponse(
                                item.getId(),
                                ItemType.TEST,
                                item.getLabTest().getId(),
                                item.getLabTest().getName(),
                                item.getLabTest().getPrice(),
                                List.of(item.getLabTest().getName())
                        )
                        : new CartResponse.CartItemResponse(
                                item.getId(),
                                ItemType.PACKAGE,
                                item.getTestPackage().getId(),
                                item.getTestPackage().getName(),
                                item.getTestPackage().getPrice(),
                                item.getTestPackage().getTests().stream()
                                        .map(LabTest::getName)
                                        .sorted()
                                        .toList()
                        ))
                .toList();

        BigDecimal total = responses.stream()
                .map(CartResponse.CartItemResponse::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(responses, responses.size(), total);
    }
}
