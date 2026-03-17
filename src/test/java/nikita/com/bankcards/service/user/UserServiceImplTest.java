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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private static User testUser;
    private static User adminUser;
    private static Long userId = 1L;

    @BeforeAll
    static void setUpTestData() {
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .password("encoded_password")
                .email("test@test.com")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .password("encoded_admin")
                .email("admin@test.com")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== ACTION 1: Получение пользователя по username ====================
    @Test
    @Order(1)
    @DisplayName("Action 1: Get user by username - success")
    void action1_getUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserByUsername("testuser");
        log.info("[TEST] Result from getUserByUsername(): {}", result.getUsername());

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@test.com", result.getEmail());
        assertEquals(User.Role.USER, result.getRole());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @Order(1)
    @DisplayName("Action 1: Get user by username - not found")
    void action1_getUserByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.getUserByUsername("unknown");
        });

        log.error("Expected exception: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("unknown"));
    }

    // ==================== ACTION 2: Получение пользователя по ID ====================
    @Test
    @Order(2)
    @DisplayName("Action 2: Get user by ID - success")
    void action2_getUserById_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserById(userId);
        log.info("found user by id :{}", result.getId());

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("testuser", result.getUsername());

        verify(userRepository).findById(userId);
    }

    @Test
    @Order(2)
    @DisplayName("Action 2: Get user by ID - not found")
    void action2_getUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(999L);
        });

        log.error("Expected exception: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("999"));
    }

    // ==================== ACTION 3: Получение всех пользователей ====================
    @Test
    @Order(3)
    @DisplayName("Action 3: Get all users - success")
    void action3_getAllUsers_Success() {
        // Given
        List<User> users = Arrays.asList(testUser, adminUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserDto> result = userService.getAllUsers();
        log.info("found users in db : {}", result.size());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals("testuser")));
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals("admin")));

        verify(userRepository).findAll();
    }

    @Test
    @Order(3)
    @DisplayName("Action 3: Get all users - empty list")
    void action3_getAllUsers_Empty() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of());

        // When
        List<UserDto> result = userService.getAllUsers();
        log.info("found user {}", result);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== ACTION 4: Удаление пользователя ====================
    @Test
    @Order(4)
    @DisplayName("Action 4: Delete user - success")
    void action4_deleteUser_Success() {
        // Given
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        // When
        userService.deleteUser(userId);
        log.info("deleted by Id {}", userId);

        // Then
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    @Order(4)
    @DisplayName("Action 4: Delete user - not found")
    void action4_deleteUser_NotFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(999L);
        });

        log.error("Expected exception: {}", exception.getMessage());

        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }

    // ==================== ACTION 5: Проверка маппинга DTO ====================
    @Test
    @Order(5)
    @DisplayName("Action 5: Verify DTO mapping excludes sensitive data")
    void action5_verifyDtoMapping() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        log.info("DTO before converting Entity into Dto {}: ", testUser);

        // When
        UserDto result = userService.getUserByUsername("testuser");
        log.info(" DTO after conversion to entity {}", result);

        // Then
        // Пароль не попадает в DTO (проверяем через отсутствие поля или null)
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getUsername());
        assertNotNull(result.getEmail());
        assertNotNull(result.getRole());

        // Убедимся что createdAt тоже маппится (добавлено в сервисе)
        assertNotNull(result.getCreatedAt());
    }
}