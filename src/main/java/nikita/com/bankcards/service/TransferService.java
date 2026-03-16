package nikita.com.bankcards.service;

import nikita.com.bankcards.dto.request.TransferRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.dto.response.TransferResponse;
import org.springframework.data.domain.Pageable;

import java.util.concurrent.CompletableFuture;

public interface TransferService {

    CompletableFuture<TransferResponse> createTransfer(TransferRequest request, String username);

    CompletableFuture<PageResponse<TransferResponse>> getUserTransfers(String username, Pageable pageable);

    CompletableFuture<PageResponse<TransferResponse>> getCardTransfers(Long cardId, Pageable pageable);
}
