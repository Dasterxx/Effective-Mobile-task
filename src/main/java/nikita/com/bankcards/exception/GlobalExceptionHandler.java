package nikita.com.bankcards.exception;

import nikita.com.bankcards.exception.card.CardAlreadyExistsException;
import nikita.com.bankcards.exception.card.CardBlockedException;
import nikita.com.bankcards.exception.card.CardNotFoundException;
import nikita.com.bankcards.exception.lock.OptimisticLockException;
import nikita.com.bankcards.exception.other.InsufficientFundsException;
import nikita.com.bankcards.exception.transfer.InvalidTransferException;
import nikita.com.bankcards.exception.other.UnauthorizedAccessException;
import nikita.com.bankcards.exception.transfer.TransferNotFoundException;
import nikita.com.bankcards.exception.user.UserAlreadyExistsException;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    /////////////////////////////// CARD EXCEPTIONS/////////////////////////
    @ExceptionHandler(CardBlockedException.class)
    public ResponseEntity<ApiResponse<String>> handleCardIsBlocked(CardBlockedException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<String>> handleCardAlreadyExist(CardAlreadyExistsException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleCardNotFound(CardNotFoundException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<String>> handleInsufficientBalance(InsufficientFundsException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    ////////////////////////////////// OTHER ////////////////////////////////////

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorizedAccess(UnauthorizedAccessException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    //////////////////////////////////////////TRANSFER EXCEPTION //////////////////////////////
    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidTransfer(InvalidTransferException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleTransferNotFound(TransferNotFoundException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /////////////////////////////////////////////LOCK //////////////////////////////////////
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse<String>> handleLock(OptimisticLockException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    ///////////////////////////////////////////////////////USER EXCEPTION/////////////////////////////////
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<String>> handleUserAlreadyExist(UserAlreadyExistsException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleUserNotFound(UserNotFoundException e) {
        ApiResponse<String> response = ApiResponse.error(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
