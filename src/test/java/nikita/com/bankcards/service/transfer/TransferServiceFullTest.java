package nikita.com.bankcards.service.transfer;

import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.dto.request.TransferRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.dto.response.TransferResponse;
import nikita.com.bankcards.entity.Card;
import nikita.com.bankcards.entity.Transfer;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.card.*;
import nikita.com.bankcards.exception.other.InsufficientFundsException;
import nikita.com.bankcards.exception.other.UnauthorizedAccessException;
import nikita.com.bankcards.exception.transfer.InvalidTransferException;
import nikita.com.bankcards.repository.CardRepository;
import nikita.com.bankcards.repository.TransferRepository;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class TransferServiceFullTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private TransferServiceImpl transferService;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "user1";

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Card blockedCard;
    private Card expiredCard;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .email("user@test.com")
                .role(User.Role.USER)
                .build();

        fromCard = Card.builder()
                .id(1L)
                .cardNumber("ENC_FROM")
                .maskedNumber("**** **** **** 1111")
                .ownerName("From Card")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .currency("RUB")
                .ownerId(USER_ID)
                .version(1L)
                .build();

        toCard = Card.builder()
                .id(2L)
                .cardNumber("ENC_TO")
                .maskedNumber("**** **** **** 2222")
                .ownerName("To Card")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .currency("RUB")
                .ownerId(USER_ID)
                .version(1L)
                .build();

        blockedCard = Card.builder()
                .id(3L)
                .cardNumber("ENC_BLOCKED")
                .maskedNumber("**** **** **** 3333")
                .ownerName("Blocked")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.BLOCKED)
                .balance(new BigDecimal("1000.00"))
                .currency("RUB")
                .ownerId(USER_ID)
                .version(1L)
                .build();

        expiredCard = Card.builder()
                .id(4L)
                .cardNumber("ENC_EXPIRED")
                .maskedNumber("**** **** **** 4444")
                .ownerName("Expired")
                .expiryDate(LocalDate.now().minusYears(1))
                .status(Card.Status.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .currency("RUB")
                .ownerId(USER_ID)
                .version(1L)
                .build();
    }

    // ==================== USER: Перевод между своими картами ====================
    @Test
    @Order(1)
    @DisplayName("Transfer Action 1: Successful transfer between own cards")
    void transfer_success() throws ExecutionException, InterruptedException {
        TransferRequest request = createRequest("1111222233334444", "5555666677778888", "100.00");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("1111222233334444")).thenReturn(CompletableFuture.completedFuture("ENC_FROM"));
        when(encryptionService.encrypt("5555666677778888")).thenReturn(CompletableFuture.completedFuture("ENC_TO"));
        when(cardRepository.findByCardNumberWithLock("ENC_FROM")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberWithLock("ENC_TO")).thenReturn(Optional.of(toCard));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> {
            Transfer t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        // When - перевод 100 рублей
        CompletableFuture<TransferResponse> future = transferService.createTransfer(request, USERNAME);
        TransferResponse result = future.get();
        log.info("Transfer response received {}", result.getAmount());

        assertEquals(Transfer.Status.COMPLETED, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals("RUB", result.getCurrency());

        // Проверяем что балансы изменились
        verify(cardRepository).save(argThat(c ->
                c.getId().equals(1L) && c.getBalance().compareTo(new BigDecimal("900.00")) == 0));
        verify(cardRepository).save(argThat(c ->
                c.getId().equals(2L) && c.getBalance().compareTo(new BigDecimal("600.00")) == 0));
    }

    @Test
    @Order(2)
    @DisplayName("Transfer Action 2: Insufficient funds")
    void transfer_insufficientFunds() throws ExecutionException, InterruptedException {
        // Given - пытаемся перевести больше чем есть
        TransferRequest request = createRequest("1111222233334444", "5555666677778888", "5000.00");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt(anyString()))
                .thenReturn(CompletableFuture.completedFuture("ENC_FROM"))
                .thenReturn(CompletableFuture.completedFuture("ENC_TO"));
        when(cardRepository.findByCardNumberWithLock("ENC_FROM")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberWithLock("ENC_TO")).thenReturn(Optional.of(toCard));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        CompletableFuture<TransferResponse> future = transferService.createTransfer(request, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(InsufficientFundsException.class, ex.getCause());
    }

    @Test
    @Order(3)
    @DisplayName("Transfer Action 3: Source card blocked")
    void transfer_sourceCardBlocked() throws ExecutionException, InterruptedException {
        TransferRequest request = createRequest("3333444455556666", "5555666677778888", "100.00");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("3333444455556666")).thenReturn(CompletableFuture.completedFuture("ENC_BLOCKED"));
        when(encryptionService.encrypt("5555666677778888")).thenReturn(CompletableFuture.completedFuture("ENC_TO"));
        when(cardRepository.findByCardNumberWithLock("ENC_BLOCKED")).thenReturn(Optional.of(blockedCard));
        when(cardRepository.findByCardNumberWithLock("ENC_TO")).thenReturn(Optional.of(toCard));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        CompletableFuture<TransferResponse> future = transferService.createTransfer(request, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(CardBlockedException.class, ex.getCause());
    }

    @Test
    @Order(4)
    @DisplayName("Transfer Action 4: Destination card expired")
    void transfer_destCardExpired() throws ExecutionException, InterruptedException {
        TransferRequest request = createRequest("1111222233334444", "7777888899990000", "100.00");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("1111222233334444")).thenReturn(CompletableFuture.completedFuture("ENC_FROM"));
        when(encryptionService.encrypt("7777888899990000")).thenReturn(CompletableFuture.completedFuture("ENC_EXPIRED"));
        when(cardRepository.findByCardNumberWithLock("ENC_FROM")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberWithLock("ENC_EXPIRED")).thenReturn(Optional.of(expiredCard));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        CompletableFuture<TransferResponse> future = transferService.createTransfer(request, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(CardExpiredException.class, ex.getCause());
    }

    @Test
    @Order(5)
    @DisplayName("Transfer Action 5: Same card transfer not allowed")
    void transfer_sameCard() throws ExecutionException, InterruptedException {
        TransferRequest request = createRequest("1111222233334444", "1111222233334444", "100.00");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("1111222233334444"))
                .thenReturn(CompletableFuture.completedFuture("ENC_FROM"))
                .thenReturn(CompletableFuture.completedFuture("ENC_FROM"));
        when(cardRepository.findByCardNumberWithLock("ENC_FROM"))
                .thenReturn(Optional.of(fromCard))
                .thenReturn(Optional.of(fromCard));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        CompletableFuture<TransferResponse> future = transferService.createTransfer(request, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(InvalidTransferException.class, ex.getCause());
    }

    @Test
    @Order(6)
    @DisplayName("Transfer Action 6: Cannot transfer to other user's card")
    void transfer_otherUserCard() throws ExecutionException, InterruptedException {
        Card otherUserCard = Card.builder()
                .id(5L)
                .cardNumber("ENC_OTHER")
                .maskedNumber("**** **** **** 5555")
                .ownerName("Other")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(Card.Status.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency("RUB")
                .ownerId(999L) // Другой пользователь
                .version(1L)
                .build();

        User otherUser = User.builder().id(999L).username("other").build();

        TransferRequest request = createRequest("1111222233334444", "9999888877776666", "100.00");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("1111222233334444")).thenReturn(CompletableFuture.completedFuture("ENC_FROM"));
        when(encryptionService.encrypt("9999888877776666")).thenReturn(CompletableFuture.completedFuture("ENC_OTHER"));
        when(cardRepository.findByCardNumberWithLock("ENC_FROM")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberWithLock("ENC_OTHER")).thenReturn(Optional.of(otherUserCard));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(999L)).thenReturn(Optional.of(otherUser));

        CompletableFuture<TransferResponse> future = transferService.createTransfer(request, USERNAME);
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        log.error(ex.getMessage(), ex);
        assertInstanceOf(UnauthorizedAccessException.class, ex.getCause());
    }

    // ==================== USER: История переводов ====================
    @Test
    @Order(7)
    @DisplayName("Transfer Action 7: View my transfer history")
    void transfer_viewHistory() throws ExecutionException, InterruptedException {
        Pageable pageable = PageRequest.of(0, 10);
        Transfer t1 = Transfer.builder()
                .id(1L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("100.00"))
                .currency("RUB")
                .status(Transfer.Status.COMPLETED)
                .build();
        Transfer t2 = Transfer.builder()
                .id(2L)
                .fromCardId(2L)
                .toCardId(1L)
                .amount(new BigDecimal("50.00"))
                .currency("RUB")
                .status(Transfer.Status.COMPLETED)
                .build();

        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(t1, t2), pageable, 2);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(transferRepository.findByUserId(USER_ID, pageable)).thenReturn(transfers);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        CompletableFuture<PageResponse<TransferResponse>> future =
                transferService.getUserTransfers(USERNAME, pageable);
        PageResponse<TransferResponse> result = future.get();
        log.info("My transfer story is {}", result.getContent().size());

        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().anyMatch(t -> t.getAmount().equals(new BigDecimal("100.00"))));
    }

    @Test
    @Order(8)
    @DisplayName("Transfer Action 8: View transfers by card")
    void transfer_viewByCard() throws ExecutionException, InterruptedException {
        Pageable pageable = PageRequest.of(0, 10);
        Transfer t1 = Transfer.builder()
                .id(1L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("100.00"))
                .status(Transfer.Status.COMPLETED)
                .build();

        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(t1), pageable, 1);

        when(cardRepository.existsById(1L)).thenReturn(true);
        when(transferRepository.findByCardId(1L, pageable)).thenReturn(transfers);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        CompletableFuture<PageResponse<TransferResponse>> future =
                transferService.getCardTransfers(1L, pageable);
        PageResponse<TransferResponse> result = future.get();
        log.info("Cards' transer stories are {}", result.getContent().size());

        assertEquals(1, result.getContent().size());
    }

    private TransferRequest createRequest(String from, String to, String amount) {
        TransferRequest request = new TransferRequest();
        request.setFromCardNumber(from);
        request.setToCardNumber(to);
        request.setAmount(new BigDecimal(amount));
        request.setDescription("Test transfer");
        return request;
    }
}
