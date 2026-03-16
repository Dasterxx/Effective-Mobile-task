package nikita.com.bankcards.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.dto.request.LoginRequest;
import nikita.com.bankcards.dto.response.AuthResponse;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.repository.UserRepository;
import nikita.com.bankcards.security.JwtTokenProvider;
import nikita.com.bankcards.service.AuthService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<AuthResponse> authenticate(LoginRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = tokenProvider.generateToken(authentication);

                User user = userRepository.findByUsername(request.getUsername())
                        .orElseThrow(() -> new UserNotFoundException("User not found"));

                log.debug("{} logged in", user.getUsername());
                log.debug("User password is {}", user.getPassword());
                log.debug("Request password is {}", request.getPassword());

                log.debug("User role: {}", user.getRole());
                return AuthResponse.builder()
                        .token(jwt)
                        .type("Bearer")
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .build();

            } catch (BadCredentialsException e) {
                throw new BadCredentialsException("Invalid username or password");
            }
        });
    }
}