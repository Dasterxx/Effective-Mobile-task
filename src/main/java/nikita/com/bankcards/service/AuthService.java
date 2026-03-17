package nikita.com.bankcards.service;

import nikita.com.bankcards.dto.request.LoginRequest;
import nikita.com.bankcards.dto.response.AuthResponse;

import java.util.concurrent.Callable;

public interface AuthService {

    AuthResponse authenticate(LoginRequest request);
}
