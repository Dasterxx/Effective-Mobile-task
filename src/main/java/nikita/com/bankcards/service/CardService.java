package nikita.com.bankcards.service;

import nikita.com.bankcards.dto.CardDto;
import nikita.com.bankcards.dto.request.CardCreateRequest;
import nikita.com.bankcards.dto.request.CardUpdateRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.concurrent.CompletableFuture;

public interface CardService {

    CompletableFuture<CardDto> createCard(CardCreateRequest request, Long userId);

    CompletableFuture<CardDto> getCard(Long id, String username);

    CompletableFuture<CardDto> getCardByMaskedNumber(String maskedNumber, String username);

    CompletableFuture<PageResponse<CardDto>> getCurrentUserCards(String username, String status, Pageable pageable);

    CompletableFuture<PageResponse<CardDto>> getAllCards(String status, Long userId, Pageable pageable);

    CompletableFuture<CardDto> updateCard(Long id, CardUpdateRequest request);

    CompletableFuture<CardDto> blockCard(Long id, String username);

    CompletableFuture<CardDto> activateCard(Long id);

    CompletableFuture<Void> deleteCard(Long id);

    CompletableFuture<CardDto> getCardEntityByNumber(String cardNumber);
}