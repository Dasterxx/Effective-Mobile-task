package nikita.com.bankcards.service;

import nikita.com.bankcards.dto.request.TransferRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.dto.response.TransferResponse;
import org.springframework.data.domain.Pageable;

public interface TransferService {
    TransferResponse createTransfer(TransferRequest request, String username);
    PageResponse<TransferResponse> getUserTransfers(String username, Pageable pageable);
    PageResponse<TransferResponse> getCardTransfers(Long cardId, Pageable pageable);
}