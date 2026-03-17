package nikita.com.bankcards.service.user;

import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.dto.UserDto;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class CardServiceUserTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@test.com";

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .email(EMAIL)
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("Action 1: Get user by username - success")
    void action1_getUserByUsername_Success() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserByUsername(USERNAME);
        log.info("Found user: {}", result.getUsername());

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        assertEquals(USERNAME, result.getUsername());
        assertEquals(EMAIL, result.getEmail());
        assertEquals(User.Role.USER, result.getRole());
    }

    @Test
    @Order(2)
    @DisplayName("Action 2: Get user by username - not found")
    void action2_getUserByUsername_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> {
            userService.getUserByUsername("unknown");
        });

        log.error("Expected exception: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("unknown"));
    }

    @Test
    @Order(3)
    @DisplayName("Action 3: Get user by ID - success")
    void action3_getUserById_Success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserById(USER_ID);
        log.info("Found user by ID: {}", result.getId());

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        assertEquals(USERNAME, result.getUsername());
    }

    @Test
    @Order(4)
    @DisplayName("Action 4: Get user by ID - not found")
    void action4_getUserById_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(999L);
        });

        log.error("Expected exception: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    @Order(5)
    @DisplayName("Action 5: Get all users")
    void action5_getAllUsers() {
        User admin = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@test.com")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, admin));

        var result = userService.getAllUsers();
        log.info("Total users: {}", result.size());

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals(USERNAME)));
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals("admin")));
    }

    @Test
    @Order(6)
    @DisplayName("Action 6: Get all users - empty list")
    void action6_getAllUsers_Empty() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        var result = userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("Action 7: Delete user - success")
    void action7_deleteUser_Success() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        doNothing().when(userRepository).deleteById(USER_ID);

        userService.deleteUser(USER_ID);
        log.info("Deleted user: {}", USER_ID);

        verify(userRepository).existsById(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @Order(8)
    @DisplayName("Action 8: Delete user - not found")
    void action8_deleteUser_NotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(999L);
        });

        log.error("Expected exception: {}", ex.getMessage());

        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }
}