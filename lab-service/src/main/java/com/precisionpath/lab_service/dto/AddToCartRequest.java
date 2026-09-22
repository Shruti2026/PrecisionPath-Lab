package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.ItemType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(

        @NotNull(message = "Item type is required")
        ItemType itemType,

        @NotNull(message = "Item id is required")
        UUID itemId
) {
}
