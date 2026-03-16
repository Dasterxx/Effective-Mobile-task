package nikita.com.bankcards.exception.card;

public class CardBlockedException extends RuntimeException {
    public CardBlockedException(String message) {
        super(message);
    }
}
