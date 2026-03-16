package nikita.com.bankcards.exception.card;

public class CardExpiredException extends RuntimeException {
    public CardExpiredException(String message) {
        super(message);
    }
}
