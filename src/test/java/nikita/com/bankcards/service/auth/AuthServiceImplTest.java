package nikita.com.bankcards.service.auth;

import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.dto.request.LoginRequest;
import nikita.com.bankcards.dto.response.AuthResponse;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.repository.UserRepository;
import nikita.com.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String TOKEN = "jwt.token.here";

    @Test
    @Order(1)
    @DisplayName("Action 1: Authenticate user successfully")
    void action1_authenticate_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);

        User user = User.builder()
                .id(1L)
                .username(USERNAME)
                .email("test@test.com")
                .role(User.Role.USER)
                .build();

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(TOKEN);
        when(userRepository.findByUsername(USERNAME)).thenReturn(java.util.Optional.of(user));

        // When
        AuthResponse response = authService.authenticate(request);  // ← просто вызов
        log.info("Auth success result {}", response);

        // Then
        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(1L, response.getUserId());
        assertEquals(USERNAME, response.getUsername());
        assertEquals("USER", response.getRole());
    }

    @Test
    @Order(2)
    @DisplayName("Action 2: Authenticate - bad credentials")
    void action2_authenticate_BadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then - проверяем прямое исключение
        assertThrows(BadCredentialsException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    @Order(3)
    @DisplayName("Action 3: Authenticate - user not found in database")
    void action3_authenticate_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword(PASSWORD);

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("token");
        when(userRepository.findByUsername("unknown")).thenReturn(java.util.Optional.empty());

        // When & Then - проверяем прямое исключение
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            authService.authenticate(request);
        });

        assertEquals("User not found", exception.getMessage());
    }
}
