package nikita.com.bankcards.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import nikita.com.bankcards.dto.request.LoginRequest;
import nikita.com.bankcards.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.service.AuthService;
import nikita.com.bankcards.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public CompletableFuture<ResponseEntity<ApiResponse<AuthResponse>>> login(
            @Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request)
                .thenApply(authResponse -> ResponseEntity.ok(ApiResponse.success(authResponse)))
                .exceptionally(ex -> ResponseEntity.badRequest()
                        .body(ApiResponse.error(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
    }
}
