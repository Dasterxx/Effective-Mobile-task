package nikita.com.bankcards.service.card;

import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.dto.CardDto;
import nikita.com.bankcards.dto.request.CardCreateRequest;
import nikita.com.bankcards.dto.request.CardUpdateRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.entity.Card;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.card.*;
import nikita.com.bankcards.exception.other.UnauthorizedAccessException;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.repository.CardRepository;
import nikita.com.bankcards.repository.UserRepository;
import nikita.com.bankcards.service.CardService;
import nikita.com.bankcards.service.EncryptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final Random random = new SecureRandom();

    @Override
    @Transactional
    public CardDto createCard(CardCreateRequest request, Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        String rawCardNumber = generateCardNumber();
        String encryptedNumber = encryptionService.encrypt(rawCardNumber).join();
        String maskedNumber = encryptionService.maskCardNumber(rawCardNumber);

        Card card = Card.builder()
                .cardNumber(encryptedNumber)
                .maskedNumber(maskedNumber)
                .ownerName(request.getOwnerName())
                .expiryDate(request.getExpiryDate())
                .currency(request.getCurrency())
                .ownerId(owner.getId())
                .build();

        Card saved = cardRepository.save(card);
        return mapToDto(saved, owner.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public CardDto getCard(Long id, String username) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        User owner = userRepository.findById(card.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException("Owner not found"));

        if (!owner.getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You don't have access to this card");
        }

        return mapToDto(card, owner.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public CardDto getCardByMaskedNumber(String maskedNumber, String username) {
        Card card = cardRepository.findByMaskedNumber(maskedNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + maskedNumber));

        User owner = userRepository.findById(card.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException("Owner not found"));

        if (!owner.getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You don't have access to this card");
        }

        return mapToDto(card, owner.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardDto> getCurrentUserCards(String username, String status, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Page<Card> page;
        if (status != null) {
            page = cardRepository.findByOwnerIdAndStatus(user.getId(), Card.Status.valueOf(status), pageable);
        } else {
            page = cardRepository.findByOwnerId(user.getId(), pageable);
        }

        return PageResponse.fromPage(page.map(card -> mapToDto(card, username)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardDto> getAllCards(String status, Long userId, Pageable pageable) {
        Page<Card> page;
        if (status != null && userId != null) {
            page = cardRepository.findByOwnerIdAndStatus(userId, Card.Status.valueOf(status), pageable);
        } else if (status != null) {
            page = cardRepository.findByStatus(Card.Status.valueOf(status), pageable);
        } else if (userId != null) {
            page = cardRepository.findByOwnerId(userId, pageable);
        } else {
            page = cardRepository.findAll(pageable);
        }

        return PageResponse.fromPage(page.map(card -> {
            User owner = userRepository.findById(card.getOwnerId()).orElse(null);
            return mapToDto(card, owner != null ? owner.getUsername() : "unknown");
        }));
    }

    @Override
    @Transactional
    public CardDto updateCard(Long id, CardUpdateRequest request) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        if (request.getStatus() != null) {
            card.setStatus(request.getStatus());
        }
        if (request.getOwnerName() != null) {
            card.setOwnerName(request.getOwnerName());
        }

        Card updated = cardRepository.save(card);
        User owner = userRepository.findById(card.getOwnerId()).orElse(null);
        return mapToDto(updated, owner != null ? owner.getUsername() : "unknown");
    }

    @Override
    @Transactional
    public CardDto blockCard(Long id, String username) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        User owner = userRepository.findById(card.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException("Owner not found"));

        if (!owner.getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You can only block your own cards");
        }

        if (card.getStatus() == Card.Status.BLOCKED) {
            throw new CardBlockedException("Card is already blocked");
        }

        card.setStatus(Card.Status.BLOCKED);
        Card updated = cardRepository.save(card);
        return mapToDto(updated, owner.getUsername());
    }

    @Override
    @Transactional
    public CardDto activateCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));

        if (card.isExpired()) {
            throw new CardExpiredException("Cannot activate expired card");
        }

        card.setStatus(Card.Status.ACTIVE);
        Card updated = cardRepository.save(card);
        User owner = userRepository.findById(card.getOwnerId()).orElse(null);
        return mapToDto(updated, owner != null ? owner.getUsername() : "unknown");
    }

    @Override
    @Transactional
    public void deleteCard(Long id) {
        if (!cardRepository.existsById(id)) {
            throw new CardNotFoundException("Card not found with id: " + id);
        }
        cardRepository.deleteById(id);
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private CardDto mapToDto(Card card, String ownerUsername) {
        return CardDto.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .ownerId(card.getOwnerId())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance() != null ? card.getBalance() : BigDecimal.ZERO)
                .currency(card.getCurrency())
                .build();
    }
}