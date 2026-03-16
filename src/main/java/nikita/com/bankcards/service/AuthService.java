package nikita.com.bankcards.service;

import nikita.com.bankcards.dto.request.LoginRequest;
import nikita.com.bankcards.dto.response.AuthResponse;

import java.util.concurrent.CompletableFuture;

public interface AuthService {

    CompletableFuture<AuthResponse> authenticate(LoginRequest request);
}
