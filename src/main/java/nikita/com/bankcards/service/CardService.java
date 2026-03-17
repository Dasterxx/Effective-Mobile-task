package nikita.com.bankcards.service;

import nikita.com.bankcards.dto.CardDto;
import nikita.com.bankcards.dto.request.CardCreateRequest;
import nikita.com.bankcards.dto.request.CardUpdateRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CardService {
    CardDto createCard(CardCreateRequest request, Long userId);
    CardDto getCard(Long id, String username);
    CardDto getCardByMaskedNumber(String maskedNumber, String username);
    PageResponse<CardDto> getCurrentUserCards(String username, String status, Pageable pageable);
    PageResponse<CardDto> getAllCards(String status, Long userId, Pageable pageable);
    CardDto updateCard(Long id, CardUpdateRequest request);
    CardDto blockCard(Long id, String username);
    CardDto activateCard(Long id);
    void deleteCard(Long id);
}