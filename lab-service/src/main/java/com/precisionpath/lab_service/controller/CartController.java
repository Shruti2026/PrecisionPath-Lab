package com.precisionpath.lab_service.controller;

import com.precisionpath.lab_service.dto.AddToCartRequest;
import com.precisionpath.lab_service.dto.CartResponse;
import com.precisionpath.lab_service.dto.MessageResponse;
import com.precisionpath.lab_service.security.AuthenticatedUser;
import com.precisionpath.lab_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patient/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        return ResponseEntity.ok(cartService.getCart(currentUser.userId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AddToCartRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.addItem(currentUser.userId(), request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID cartItemId
    ) {

        return ResponseEntity.ok(cartService.removeItem(currentUser.userId(), cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> clearCart(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        cartService.clear(currentUser.userId());

        return ResponseEntity.ok(new MessageResponse("Cart cleared"));
    }
}
