package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.AppointmentItem;
import com.precisionpath.lab_service.entity.ItemType;

import java.math.BigDecimal;
import java.util.UUID;

public record AppointmentItemResponse(
        ItemType itemType,
        UUID itemId,
        String name,
        BigDecimal price
) {

    public static AppointmentItemResponse from(AppointmentItem item) {
        UUID itemId = item.getItemType() == ItemType.TEST
                ? item.getLabTest().getId()
                : item.getTestPackage().getId();

        return new AppointmentItemResponse(
                item.getItemType(),
                itemId,
                item.getItemName(),
                item.getPrice()
        );
    }
}
