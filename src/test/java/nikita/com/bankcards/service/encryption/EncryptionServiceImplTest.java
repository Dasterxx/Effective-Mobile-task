package nikita.com.bankcards.service.encryption;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EncryptionServiceImplTest {

    @InjectMocks
    private EncryptionServiceImpl encryptionService;

    private static final String TEST_KEY = "MySecretKey123456";
    private static final String TEST_CARD_NUMBER = "1234567890123456";

    @BeforeEach
    void setUp() {
        // Устанавливаем ключ через Reflection, так как @Value не работает в тестах без контекста
        ReflectionTestUtils.setField(encryptionService, "secretKey", TEST_KEY);
    }

    // ==================== ACTION 1: Шифрование ====================
    @Test
    @Order(1)
    @DisplayName("Action 1: Encrypt card number successfully")
    void action1_encrypt_Success() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<String> future = encryptionService.encrypt(TEST_CARD_NUMBER);
        String encrypted = future.get();

        // Then
        assertNotNull(encrypted);
        assertNotEquals(TEST_CARD_NUMBER, encrypted);
        assertFalse(encrypted.contains(TEST_CARD_NUMBER));
        log.info("Encrypted: {} -> {}", TEST_CARD_NUMBER, encrypted);
    }

    @Test
    @Order(1)
    @DisplayName("Action 1: Encrypt null returns null")
    void action1_encrypt_NullReturnsNull() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<String> future = encryptionService.encrypt(null);
        String result = future.get();

        // Then
        assertNull(result);
    }

    @Test
    @Order(1)
    @DisplayName("Action 1: Encrypt empty returns empty")
    void action1_encrypt_EmptyReturnsEmpty() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<String> future = encryptionService.encrypt("");
        String result = future.get();

        // Then
        assertEquals("", result);
    }

    // ==================== ACTION 2: Дешифрование ====================
    @Test
    @Order(2)
    @DisplayName("Action 2: Decrypt encrypted card number")
    void action2_decrypt_Success() throws ExecutionException, InterruptedException {
        // Given
        String encrypted = encryptionService.encrypt(TEST_CARD_NUMBER).get();

        // When
        CompletableFuture<String> future = encryptionService.decrypt(encrypted);
        String decrypted = future.get();

        // Then
        assertEquals(TEST_CARD_NUMBER, decrypted);
        log.info("Decrypted: {} -> {}", encrypted, decrypted);
    }

    @Test
    @Order(2)
    @DisplayName("Action 2: Decrypt null returns null")
    void action2_decrypt_NullReturnsNull() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<String> future = encryptionService.decrypt(null);
        String result = future.get();

        // Then
        assertNull(result);
    }

    @Test
    @Order(2)
    @DisplayName("Action 2: Decrypt empty returns empty")
    void action2_decrypt_EmptyReturnsEmpty() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<String> future = encryptionService.decrypt("");
        String result = future.get();

        // Then
        assertEquals("", result);
    }

    @Test
    @Order(2)
    @DisplayName("Action 2: Decrypt with wrong key fails")
    void action2_decrypt_WrongKeyFails() throws ExecutionException, InterruptedException {
        // Given
        String encrypted = encryptionService.encrypt(TEST_CARD_NUMBER).get();

        // Меняем ключ на другой
        ReflectionTestUtils.setField(encryptionService, "secretKey", "WrongKey12345678");

        // When & Then
        CompletableFuture<String> future = encryptionService.decrypt(encrypted);
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("Decryption failed"));
    }

    // ==================== ACTION 3: Маскирование ====================
    @Test
    @Order(3)
    @DisplayName("Action 3: Mask plain card number")
    void action3_maskPlain_Success() {
        // When
        String masked = encryptionService.maskCardNumber(TEST_CARD_NUMBER);

        // Then
        assertEquals("**** **** **** 3456", masked);
        log.info("Masked plain: {} -> {}", TEST_CARD_NUMBER, masked);
    }

    @Test
    @Order(3)
    @DisplayName("Action 3: Mask encrypted card number")
    void action3_maskEncrypted_Success() throws ExecutionException, InterruptedException {
        // Given
        String encrypted = encryptionService.encrypt(TEST_CARD_NUMBER).get();

        // When
        String masked = encryptionService.maskCardNumber(encrypted);

        // Then
        assertEquals("**** **** **** 3456", masked);
        assertEquals(19, masked.length());
        log.info("Masked encrypted: {} -> {}", encrypted, masked);
    }

    @Test
    @Order(3)
    @DisplayName("Action 3: Mask null returns null")
    void action3_maskNull_ReturnsNull() {
        // When
        String masked = encryptionService.maskCardNumber(null);

        // Then
        assertNull(masked);
    }

    @Test
    @Order(3)
    @DisplayName("Action 3: Mask short number returns as is")
    void action3_maskShort_ReturnsAsIs() {
        // Given
        String shortNumber = "123";

        // When
        String masked = encryptionService.maskCardNumber(shortNumber);

        // Then
        assertEquals("123", masked);
    }

    @Test
    @Order(3)
    @DisplayName("Action 3: Mask number with spaces")
    void action3_maskWithSpaces() {
        // Given
        String numberWithSpaces = "1234 5678 9012 3456";

        // When
        String masked = encryptionService.maskCardNumber(numberWithSpaces);

        // Then
        assertEquals("**** **** **** 3456", masked);
    }

    // ==================== ACTION 4: Полный цикл ====================
    @Test
    @Order(4)
    @DisplayName("Action 4: Full cycle - encrypt, decrypt, mask")
    void action4_fullCycle() throws ExecutionException, InterruptedException {
        // Given
        String original = "9876543210987654";

        // When
        String encrypted = encryptionService.encrypt(original).get();
        String decrypted = encryptionService.decrypt(encrypted).get();
        String masked = encryptionService.maskCardNumber(encrypted);

        // Then
        assertNotEquals(original, encrypted);
        assertEquals(original, decrypted);
        assertEquals("**** **** **** 7654", masked);

        log.info("Full cycle: {} -> {} -> {} -> {}",
                original, encrypted, decrypted, masked);
    }

    // ==================== ACTION 5: Разные номера карт ====================
    @Test
    @Order(5)
    @DisplayName("Action 5: Encrypt different card numbers")
    void action5_differentCards() throws ExecutionException, InterruptedException {
        // Given
        String[] cards = {
                "1234567890123456",
                "5555555555554444",
                "4111111111111111",
                "4000000000000002"
        };

        for (String card : cards) {
            // When
            String encrypted = encryptionService.encrypt(card).get();
            String decrypted = encryptionService.decrypt(encrypted).get();
            String masked = encryptionService.maskCardNumber(card);

            // Then
            assertEquals(card, decrypted);
            assertTrue(masked.startsWith("**** **** **** "));
            assertEquals(19, masked.length());

            log.info("Card: {} -> Encrypted: {} -> Masked: {}", card, encrypted, masked);
        }
    }
}
