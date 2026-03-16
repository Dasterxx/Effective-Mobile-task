package nikita.com.bankcards.exception.transfer;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(String message) {
        super(message);
    }
}
