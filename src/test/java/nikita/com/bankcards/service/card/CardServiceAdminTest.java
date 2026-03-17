package nikita.com.bankcards.service.card;

import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.dto.CardDto;
import nikita.com.bankcards.dto.request.CardCreateRequest;
import nikita.com.bankcards.dto.request.CardUpdateRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.entity.Card;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.card.CardExpiredException;
import nikita.com.bankcards.exception.card.CardNotFoundException;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.repository.CardRepository;
import nikita.com.bankcards.repository.UserRepository;
import nikita.com.bankcards.service.EncryptionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class CardServiceAdminTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private CardServiceImpl cardService;

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final String ADMIN_USERNAME = "admin";
    private static final String USER_USERNAME = "user";

    private User adminUser;
    private User regularUser;
    private Card userCard;
    private Card expiredCard;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(ADMIN_ID)
                .username(ADMIN_USERNAME)
                .email("admin@bank.com")
                .role(User.Role.ADMIN)
                .build();

        regularUser = User.builder()
                .id(USER_ID)
                .username(USER_USERNAME)
                .email("user@bank.com")
                .role(User.Role.USER)
                .build();

        userCard = Card.builder()
                .id(1L)
                .cardNumber("ENC_1234")
                .maskedNumber("**** **** **** 1234")
                .ownerName("User Card")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.ACTIVE)
                .balance(new BigDecimal("5000.00"))
                .currency("RUB")
                .ownerId(USER_ID)
                .build();

        expiredCard = Card.builder()
                .id(2L)
                .cardNumber("ENC_5678")
                .maskedNumber("**** **** **** 5678")
                .ownerName("Expired Card")
                .expiryDate(LocalDate.now().minusMonths(1))
                .status(Card.Status.EXPIRED)
                .balance(BigDecimal.ZERO)
                .currency("RUB")
                .ownerId(USER_ID)
                .build();
    }

    // ==================== ADMIN: Создание карты ====================
    @Test
    @Order(1)
    @DisplayName("Admin Action 1: Create card for user - verifies encryption is called")
    void admin_createCard_ForUser() {
        // Given
        CardCreateRequest request = new CardCreateRequest();
        request.setOwnerName("New Card Owner");
        request.setExpiryDate(LocalDate.now().plusYears(3));
        request.setCurrency("USD");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularUser));
        when(encryptionService.encrypt(anyString()))
                .thenReturn(CompletableFuture.completedFuture("ANY_ENCRYPTED_VALUE"));
        when(encryptionService.maskCardNumber(anyString()))
                .thenReturn("**** **** **** 9999");
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        // When
        CardDto result = cardService.createCard(request, USER_ID);

        log.info("Created card DTO: id={}, status={}, currency={}, balance={}",
                result.getId(), result.getStatus(), result.getCurrency(), result.getBalance());

        // Then
        assertNotNull(result);
        assertEquals(Card.Status.ACTIVE, result.getStatus());
        assertEquals("USD", result.getCurrency());
        assertEquals(BigDecimal.ZERO, result.getBalance());

        verify(encryptionService).encrypt(anyString());
        verify(encryptionService).maskCardNumber(anyString());
        verify(cardRepository).save(argThat(card ->
                card.getOwnerId().equals(USER_ID) &&
                        card.getStatus() == Card.Status.ACTIVE &&
                        card.getCardNumber() != null &&
                        card.getMaskedNumber() != null &&
                        card.getBalance().equals(BigDecimal.ZERO)
        ));
    }

    @Test
    @Order(2)
    @DisplayName("Admin Action 2: Create card - user not found")
    void admin_createCard_UserNotFound() {
        // Given
        CardCreateRequest request = new CardCreateRequest();
        request.setOwnerName("Test Owner");
        request.setExpiryDate(LocalDate.now().plusYears(2));
        request.setCurrency("RUB");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            cardService.createCard(request, 999L);
        });

        log.error("Expected exception: {}", exception.getMessage());

        verifyNoInteractions(encryptionService);
        verifyNoInteractions(cardRepository);
    }

    // ==================== ADMIN: Активация карты ====================
    @Test
    @Order(3)
    @DisplayName("Admin Action 3: Activate blocked card")
    void admin_activateCard_Blocked() {
        // Given
        Card blockedCard = Card.builder()
                .id(3L)
                .cardNumber("ENC_BLOCKED")
                .maskedNumber("**** **** **** 3333")
                .ownerName("Blocked")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(Card.Status.BLOCKED)
                .balance(BigDecimal.TEN)
                .currency("RUB")
                .ownerId(USER_ID)
                .build();

        when(cardRepository.findById(3L)).thenReturn(Optional.of(blockedCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularUser));

        // When
        CardDto result = cardService.activateCard(3L);
        log.info("Activated card ID {}: ", result.getId());

        // Then
        assertEquals(Card.Status.ACTIVE, result.getStatus());
        verify(cardRepository).save(argThat(card -> card.getStatus() == Card.Status.ACTIVE));
    }

    @Test
    @Order(4)
    @DisplayName("Admin Action 4: Cannot activate expired card")
    void admin_activateCard_ExpiredFails() {
        // Given
        when(cardRepository.findById(2L)).thenReturn(Optional.of(expiredCard));

        // When & Then
        CardExpiredException ex = assertThrows(CardExpiredException.class, () -> {
            cardService.activateCard(2L);
        });
        log.error("Expected exception: {}", ex.getMessage());
    }

    // ==================== ADMIN: Удаление карты ====================
    @Test
    @Order(5)
    @DisplayName("Admin Action 5: Delete card")
    void admin_deleteCard_Success() {
        // Given
        when(cardRepository.existsById(1L)).thenReturn(true);
        doNothing().when(cardRepository).deleteById(1L);

        // When
        cardService.deleteCard(1L);
        log.info("[TEST] Deleted card {} successfully.", 1);

        // Then
        verify(cardRepository).existsById(1L);
        verify(cardRepository).deleteById(1L);
    }

    @Test
    @Order(6)
    @DisplayName("Admin Action 6: Delete non-existent card")
    void admin_deleteCard_NotFound() {
        // Given
        when(cardRepository.existsById(999L)).thenReturn(false);

        // When & Then
        CardNotFoundException ex = assertThrows(CardNotFoundException.class, () -> {
            cardService.deleteCard(999L);
        });
        log.error("Expected exception: {}", ex.getMessage());
    }

    // ==================== ADMIN: Просмотр всех карт ====================
    @Test
    @Order(7)
    @DisplayName("Admin Action 7: View all cards with pagination")
    void admin_viewAllCards_Pagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> allCards = new PageImpl<>(Arrays.asList(userCard, expiredCard), pageable, 2);

        when(cardRepository.findAll(pageable)).thenReturn(allCards);
        when(userRepository.findById(any())).thenReturn(Optional.of(regularUser));

        // When
        PageResponse<CardDto> result = cardService.getAllCards(null, null, pageable);
        log.info("all cards size :{}", result.getTotalElements());

        // Then
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream()
                .anyMatch(c -> c.getStatus() == Card.Status.ACTIVE));
        assertTrue(result.getContent().stream()
                .anyMatch(c -> c.getStatus() == Card.Status.EXPIRED));

        verifyNoInteractions(encryptionService);
    }

    @Test
    @Order(8)
    @DisplayName("Admin Action 8: Filter cards by status")
    void admin_viewAllCards_FilterByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> activeCards = new PageImpl<>(Collections.singletonList(userCard), pageable, 1);

        when(cardRepository.findByStatus(Card.Status.ACTIVE, pageable)).thenReturn(activeCards);
        when(userRepository.findById(any())).thenReturn(Optional.of(regularUser));

        // When
        PageResponse<CardDto> result = cardService.getAllCards("ACTIVE", null, pageable);
        log.info("active cards count by status {} ", result.getContent().size());

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(Card.Status.ACTIVE, result.getContent().get(0).getStatus());
    }

    @Test
    @Order(9)
    @DisplayName("Admin Action 9: Filter cards by user ID")
    void admin_viewAllCards_FilterByUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> userCards = new PageImpl<>(Arrays.asList(userCard, expiredCard), pageable, 2);

        when(cardRepository.findByOwnerId(USER_ID, pageable)).thenReturn(userCards);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularUser));

        // When
        PageResponse<CardDto> result = cardService.getAllCards(null, USER_ID, pageable);
        log.info("filtered cards by userId {}", result.getContent().size());

        // Then
        assertEquals(2, result.getContent().size());
    }

    // ==================== ADMIN: Обновление карты ====================
    @Test
    @Order(10)
    @DisplayName("Admin Action 10: Update card details")
    void admin_updateCard() {
        // Given
        CardUpdateRequest request = new CardUpdateRequest();
        request.setOwnerName("Updated Owner Name");
        request.setStatus(Card.Status.BLOCKED);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(userCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularUser));

        // When
        log.info("old card {}", request);
        CardDto result = cardService.updateCard(1L, request);
        log.info("updated card {}", result);

        // Then
        assertEquals(USER_ID, result.getOwnerId());
        assertEquals(Card.Status.BLOCKED, result.getStatus());

        verifyNoInteractions(encryptionService);
    }
}