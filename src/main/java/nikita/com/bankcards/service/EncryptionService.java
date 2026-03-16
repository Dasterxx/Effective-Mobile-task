package nikita.com.bankcards.service;

import java.util.concurrent.CompletableFuture;

public interface EncryptionService {

    CompletableFuture<String> encrypt(String data);

    CompletableFuture<String> decrypt(String encryptedData);

    String maskCardNumber(String cardNumber);
}
