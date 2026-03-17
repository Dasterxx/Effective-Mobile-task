package nikita.com.bankcards.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.dto.CardDto;
import nikita.com.bankcards.dto.request.CardCreateRequest;
import nikita.com.bankcards.dto.request.CardUpdateRequest;
import nikita.com.bankcards.dto.response.PageResponse;
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
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<CardDto>> createCard(
            @Valid @RequestBody CardCreateRequest request,
            @RequestParam Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardService.createCard(request, userId)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my cards", description = "Get current user's cards with pagination")
    public ResponseEntity<ApiResponse<PageResponse<CardDto>>> getMyCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getCurrentUserCards(userDetails.getUsername(), status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get card by ID")
    public ResponseEntity<ApiResponse<CardDto>> getCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getCard(id, userDetails.getUsername())));
    }

    @GetMapping("/number/{maskedNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get card by masked number")
    public ResponseEntity<ApiResponse<CardDto>> getCardByNumber(
            @PathVariable String maskedNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getCardByMaskedNumber(maskedNumber, userDetails.getUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update card", description = "Admin only: Update card details")
    public ResponseEntity<ApiResponse<CardDto>> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cardService.updateCard(id, request)));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Block card", description = "User can block own card, admin can block any")
    public ResponseEntity<ApiResponse<CardDto>> blockCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.blockCard(id, userDetails.getUsername())));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate card", description = "Admin only: Activate blocked or expired card")
    public ResponseEntity<ApiResponse<CardDto>> activateCard(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(cardService.activateCard(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete card", description = "Admin only: Delete card from system")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all cards", description = "Admin only: Get all cards with pagination and filters")
    public ResponseEntity<ApiResponse<PageResponse<CardDto>>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getAllCards(status, userId, pageable)));
    }
}