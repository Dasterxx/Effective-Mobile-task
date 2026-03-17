package nikita.com.bankcards.service.encryption;

import nikita.com.bankcards.service.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final String ALGORITHM = "AES";

    @Value("${app.encryption.key:MySecretKey123456}")
    private String secretKey;

    @Override
    @Async
    public CompletableFuture<String> encrypt(String data) {
        return CompletableFuture.supplyAsync(() -> {
            if (data == null || data.isEmpty()) {
                return data;
            }
            try {
                SecretKeySpec key = new SecretKeySpec(getKeyBytes(), ALGORITHM);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(encrypted);
            } catch (Exception e) {
                throw new RuntimeException("Encryption failed", e);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<String> decrypt(String encryptedData) {
        return CompletableFuture.supplyAsync(() -> {
            if (encryptedData == null || encryptedData.isEmpty()) {
                return encryptedData;
            }
            try {
                SecretKeySpec key = new SecretKeySpec(getKeyBytes(), ALGORITHM);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key);
                byte[] decoded = Base64.getDecoder().decode(encryptedData);
                byte[] decrypted = cipher.doFinal(decoded);
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Decryption failed", e);
            }
        });
    }

    @Override
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        String cleanNumber = cardNumber;
        if (cardNumber.length() > 16) {
            try {
                cleanNumber = decrypt(cardNumber).join();
            } catch (Exception e) {
                cleanNumber = cardNumber;
            }
        }

        String last4 = cleanNumber.replaceAll("\\D", "");
        if (last4.length() >= 4) {
            last4 = last4.substring(last4.length() - 4);
        }
        return "**** **** **** " + last4;
    }

    private byte[] getKeyBytes() {
        String key = String.format("%-16s", secretKey).substring(0, 16);
        return key.getBytes(StandardCharsets.UTF_8);
    }
}