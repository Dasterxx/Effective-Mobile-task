package nikita.com.bankcards.controller;

import nikita.com.bankcards.dto.CardDto;
import nikita.com.bankcards.dto.request.CardCreateRequest;
import nikita.com.bankcards.dto.request.CardUpdateRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.service.CardService;
import nikita.com.bankcards.util.ApiResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new card", description = "Admin only: Create a new card for user")
    public CompletableFuture<ResponseEntity<ApiResponse<CardDto>>> createCard(
            @Valid @RequestBody CardCreateRequest request,
            @RequestParam Long userId) {
        return cardService.createCard(request, userId)
                .thenApply(cardDto -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(cardDto)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my cards", description = "Get current user's cards with pagination")
    public CompletableFuture<ResponseEntity<ApiResponse<PageResponse<CardDto>>>> getMyCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return cardService.getCurrentUserCards(userDetails.getUsername(), status, pageable)
                .thenApply(pageResponse -> ResponseEntity.ok(ApiResponse.success(pageResponse)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get card by ID")
    public CompletableFuture<ResponseEntity<ApiResponse<CardDto>>> getCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return cardService.getCard(id, userDetails.getUsername())
                .thenApply(cardDto -> ResponseEntity.ok(ApiResponse.success(cardDto)));
    }

    @GetMapping("/number/{maskedNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get card by masked number")
    public CompletableFuture<ResponseEntity<ApiResponse<CardDto>>> getCardByNumber(
            @PathVariable String maskedNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return cardService.getCardByMaskedNumber(maskedNumber, userDetails.getUsername())
                .thenApply(cardDto -> ResponseEntity.ok(ApiResponse.success(cardDto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update card", description = "Admin only: Update card details")
    public CompletableFuture<ResponseEntity<ApiResponse<CardDto>>> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardUpdateRequest request) {
        return cardService.updateCard(id, request)
                .thenApply(cardDto -> ResponseEntity.ok(ApiResponse.success(cardDto)));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Block card", description = "User can block own card, admin can block any")
    public CompletableFuture<ResponseEntity<ApiResponse<CardDto>>> blockCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return cardService.blockCard(id, userDetails.getUsername())
                .thenApply(cardDto -> ResponseEntity.ok(ApiResponse.success(cardDto)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate card", description = "Admin only: Activate blocked or expired card")
    public CompletableFuture<ResponseEntity<ApiResponse<CardDto>>> activateCard(@PathVariable Long id) {
        return cardService.activateCard(id)
                .thenApply(cardDto -> ResponseEntity.ok(ApiResponse.success(cardDto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete card", description = "Admin only: Delete card from system")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteCard(@PathVariable Long id) {
        return cardService.deleteCard(id)
                .thenApply(v -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all cards", description = "Admin only: Get all cards with pagination and filters")
    public CompletableFuture<ResponseEntity<ApiResponse<PageResponse<CardDto>>>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return cardService.getAllCards(status, userId, pageable)
                .thenApply(pageResponse -> ResponseEntity.ok(ApiResponse.success(pageResponse)));
    }
}
