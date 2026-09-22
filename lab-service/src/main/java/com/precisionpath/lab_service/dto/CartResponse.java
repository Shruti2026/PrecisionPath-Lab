package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.ItemType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        List<CartItemResponse> items,
        int itemCount,
        BigDecimal totalAmount
) {

    public record CartItemResponse(
            UUID cartItemId,
            ItemType itemType,
            UUID itemId,
            String name,
            BigDecimal price,
            List<String> includedTests
    ) {
    }
}
