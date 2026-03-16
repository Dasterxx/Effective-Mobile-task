package nikita.com.bankcards.controller;

import nikita.com.bankcards.dto.request.TransferRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.dto.response.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.service.TransferService;
import nikita.com.bankcards.util.ApiResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Money transfer endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create transfer", description = "Transfer money between own cards")
    public CompletableFuture<ResponseEntity<ApiResponse<TransferResponse>>> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return transferService.createTransfer(request, userDetails.getUsername())
                .thenApply(transferResponse -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(transferResponse)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my transfers", description = "Get transfer history for current user")
    public CompletableFuture<ResponseEntity<ApiResponse<PageResponse<TransferResponse>>>> getMyTransfers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transferService.getUserTransfers(userDetails.getUsername(), pageable)
                .thenApply(pageResponse -> ResponseEntity.ok(ApiResponse.success(pageResponse)));
    }

    @GetMapping("/card/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get card transfers", description = "Get transfer history for specific card")
    public CompletableFuture<ResponseEntity<ApiResponse<PageResponse<TransferResponse>>>> getCardTransfers(
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transferService.getCardTransfers(cardId, pageable)
                .thenApply(pageResponse -> ResponseEntity.ok(ApiResponse.success(pageResponse)));
    }
}
