package nikita.com.bankcards.service.card;

import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.dto.CardDto;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.entity.Card;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.card.CardBlockedException;
import nikita.com.bankcards.exception.card.CardNotFoundException;
import nikita.com.bankcards.exception.other.UnauthorizedAccessException;
import nikita.com.bankcards.repository.CardRepository;
import nikita.com.bankcards.repository.UserRepository;
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
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class CardServiceUserTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String USERNAME = "user1";
    private static final String OTHER_USERNAME = "user2";

    private User testUser;
    private User otherUser;
    private Card myCard1;
    private Card myCard2;
    private Card otherUserCard;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .email("user1@test.com")
                .role(User.Role.USER)
                .build();

        otherUser = User.builder()
                .id(OTHER_USER_ID)
                .username(OTHER_USERNAME)
                .email("user2@test.com")
                .role(User.Role.USER)
                .build();

        myCard1 = Card.builder()
                .id(1L)
                .cardNumber("ENC_MINE1")
                .maskedNumber("**** **** **** 1111")
                .ownerName("My Card 1")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.ACTIVE)
                .balance(new BigDecimal("5000.00"))
                .currency("RUB")
                .ownerId(USER_ID)
                .build();

        myCard2 = Card.builder()
                .id(2L)
                .cardNumber("ENC_MINE2")
                .maskedNumber("**** **** **** 2222")
                .ownerName("My Card 2")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(Card.Status.BLOCKED)
                .balance(new BigDecimal("1000.00"))
                .currency("RUB")
                .ownerId(USER_ID)
                .build();

        otherUserCard = Card.builder()
                .id(3L)
                .cardNumber("ENC_OTHER")
                .maskedNumber("**** **** **** 3333")
                .ownerName("Other Card")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.ACTIVE)
                .balance(new BigDecimal("9999.00"))
                .currency("RUB")
                .ownerId(OTHER_USER_ID)
                .build();
    }

    // ==================== USER: Просмотр своих карт ====================
    @Test
    @Order(1)
    @DisplayName("User Action 1: View my cards with pagination")
    void user_viewMyCards_Pagination() throws ExecutionException, InterruptedException {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> myCards = new PageImpl<>(Arrays.asList(myCard1, myCard2), pageable, 2);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwnerId(USER_ID, pageable)).thenReturn(myCards);

        CompletableFuture<PageResponse<CardDto>> future =
                cardService.getCurrentUserCards(USERNAME, null, pageable);
        PageResponse<CardDto> result = future.get();
        log.info("My card {}", result);

        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(c ->
                c.getMaskedNumber().equals("**** **** **** 1111") ||
                        c.getMaskedNumber().equals("**** **** **** 2222")));
    }

    @Test
    @Order(2)
    @DisplayName("User Action 2: View my cards filtered by status")
    void user_viewMyCards_FilterByStatus() throws ExecutionException, InterruptedException {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> activeCards = new PageImpl<>(Collections.singletonList(myCard1), pageable, 1);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwnerIdAndStatus(USER_ID, Card.Status.ACTIVE, pageable))
                .thenReturn(activeCards);

        CompletableFuture<PageResponse<CardDto>> future =
                cardService.getCurrentUserCards(USERNAME, "ACTIVE", pageable);
        PageResponse<CardDto> result = future.get();
        log.info("Active cards {}: ",result.getContent().size());

        assertEquals(1, result.getContent().size());
        assertEquals(Card.Status.ACTIVE, result.getContent().get(0).getStatus());
    }

    // ==================== USER: Просмотр баланса ====================
    @Test
    @Order(3)
    @DisplayName("User Action 3: View card balance")
    void user_viewCardBalance() throws ExecutionException, InterruptedException {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(myCard1));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        CompletableFuture<CardDto> future = cardService.getCard(1L, USERNAME);
        CardDto result = future.get();
        log.info("Result Balance :{}", result.getBalance());

        assertEquals(new BigDecimal("5000.00"), result.getBalance());
    }

    // ==================== USER: Блокировка своей карты ====================
    @Test
    @Order(4)
    @DisplayName("User Action 4: Block my own card")
    void user_blockMyCard_Success() throws ExecutionException, InterruptedException {
        // Given - создаем активную карту для блокировки
        Card cardToBlock = Card.builder()
                .id(10L)
                .cardNumber("ENC_BLOCKME")
                .maskedNumber("**** **** **** 4444")
                .ownerName("Block Me")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.ACTIVE)
                .balance(BigDecimal.TEN)
                .currency("RUB")
                .ownerId(USER_ID)
                .build();
        log.info("card status {}", cardToBlock.getStatus());
        when(cardRepository.findById(10L)).thenReturn(Optional.of(cardToBlock));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CompletableFuture<CardDto> future = cardService.blockCard(10L, USERNAME);
        CardDto result = future.get();
        log.info("card status {}", result.getStatus());

        assertEquals(Card.Status.BLOCKED, result.getStatus());
        verify(cardRepository).save(argThat(c -> c.getStatus() == Card.Status.BLOCKED));
    }

    @Test
    @Order(5)
    @DisplayName("User Action 5: Cannot block already blocked card")
    void user_blockCard_AlreadyBlocked() {
        when(cardRepository.findById(2L)).thenReturn(Optional.of(myCard2));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        CompletableFuture<CardDto> future = cardService.blockCard(2L, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(CardBlockedException.class, ex.getCause());
    }

    @Test
    @Order(6)
    @DisplayName("User Action 6: Cannot block other user's card")
    void user_blockCard_OtherUserCard() {
        when(cardRepository.findById(3L)).thenReturn(Optional.of(otherUserCard));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

        CompletableFuture<CardDto> future = cardService.blockCard(3L, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(UnauthorizedAccessException.class, ex.getCause());
    }

    // ==================== USER: Поиск карты по маскированному номеру ====================
    @Test
    @Order(7)
    @DisplayName("User Action 7: Find card by masked number")
    void user_findByMaskedNumber() throws ExecutionException, InterruptedException {
        when(cardRepository.findByMaskedNumber("**** **** **** 1111")).thenReturn(Optional.of(myCard1));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        CompletableFuture<CardDto> future =
                cardService.getCardByMaskedNumber("**** **** **** 1111", USERNAME);
        CardDto result = future.get();
        log.info("found a crd by Mask Number {}", result.getMaskedNumber());

        assertNotNull(result);
        assertEquals("**** **** **** 1111", result.getMaskedNumber());
    }

    @Test
    @Order(8)
    @DisplayName("User Action 8: Cannot access other user's card by masked number")
    void user_findByMaskedNumber_OtherUser() {
        when(cardRepository.findByMaskedNumber("**** **** **** 3333")).thenReturn(Optional.of(otherUserCard));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

        CompletableFuture<CardDto> future =
                cardService.getCardByMaskedNumber("**** **** **** 3333", USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(UnauthorizedAccessException.class, ex.getCause());
    }

    // ==================== USER: Нет доступа к чужим картам по ID ====================
    @Test
    @Order(9)
    @DisplayName("User Action 9: Cannot get other user's card by ID")
    void user_getCardById_OtherUser() {
        when(cardRepository.findById(3L)).thenReturn(Optional.of(otherUserCard));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

        CompletableFuture<CardDto> future = cardService.getCard(3L, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(UnauthorizedAccessException.class, ex.getCause());
    }

    // ==================== USER: Карта не найдена ====================
    @Test
    @Order(10)
    @DisplayName("User Action 10: Card not found")
    void user_cardNotFound() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        CompletableFuture<CardDto> future = cardService.getCard(999L, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(CardNotFoundException.class, ex.getCause());
    }
}
