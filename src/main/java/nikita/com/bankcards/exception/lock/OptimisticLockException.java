package nikita.com.bankcards.exception.lock;

public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String message) {
        super(message);
    }
}
