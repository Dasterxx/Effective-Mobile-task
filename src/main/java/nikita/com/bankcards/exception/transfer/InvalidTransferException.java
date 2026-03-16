package nikita.com.bankcards.exception.transfer;

public class InvalidTransferException extends RuntimeException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
