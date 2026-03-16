package nikita.com.bankcards.controller;

import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.service.UserService;
import nikita.com.bankcards.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get current user info")
    public CompletableFuture<ResponseEntity<ApiResponse<UserDto>>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername())
                .thenApply(userDto -> ResponseEntity.ok(ApiResponse.success(userDto)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Admin only: Get all users")
    public CompletableFuture<ResponseEntity<ApiResponse<List<UserDto>>>> getAllUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("🔥 CONTROLLER CALLED! Auth principal: {}",
                auth != null ? auth.getName() : "NULL ❌");
        return userService.getAllUsers()
                .thenApply(users -> ResponseEntity.ok(ApiResponse.success(users)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Admin only")
    public CompletableFuture<ResponseEntity<ApiResponse<UserDto>>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .thenApply(userDto -> ResponseEntity.ok(ApiResponse.success(userDto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Admin only: Delete user and all cards")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                .thenApply(v -> ResponseEntity.ok(ApiResponse.success(null)));
    }
}

