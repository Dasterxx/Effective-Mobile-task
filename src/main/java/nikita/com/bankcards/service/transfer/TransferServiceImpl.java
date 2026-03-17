package nikita.com.bankcards.service.transfer;

import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.dto.request.TransferRequest;
import nikita.com.bankcards.dto.response.PageResponse;
import nikita.com.bankcards.dto.response.TransferResponse;
import nikita.com.bankcards.entity.Card;
import nikita.com.bankcards.entity.Transfer;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.card.*;
import nikita.com.bankcards.exception.lock.OptimisticLockException;
import nikita.com.bankcards.exception.other.InsufficientFundsException;
import nikita.com.bankcards.exception.other.UnauthorizedAccessException;
import nikita.com.bankcards.exception.transfer.InvalidTransferException;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.repository.CardRepository;
import nikita.com.bankcards.repository.TransferRepository;
import nikita.com.bankcards.repository.UserRepository;
import nikita.com.bankcards.service.EncryptionService;
import nikita.com.bankcards.service.TransferService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public TransferResponse createTransfer(TransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        String fromCardNumber = encryptionService.encrypt(request.getFromCardNumber()).join();
        String toCardNumber = encryptionService.encrypt(request.getToCardNumber()).join();

        Card fromCard = cardRepository.findByCardNumberWithLock(fromCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Source card not found"));
        Card toCard = cardRepository.findByCardNumberWithLock(toCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Destination card not found"));

        User fromCardOwner = userRepository.findById(fromCard.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException("Owner of source card not found"));
        User toCardOwner = userRepository.findById(toCard.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException("Owner of destination card not found"));

        if (!fromCardOwner.getId().equals(user.getId()) ||
                !toCardOwner.getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You can only transfer between your own cards");
        }

        if (fromCard.getStatus() == Card.Status.BLOCKED) {
            throw new CardBlockedException("Source card is blocked");
        }
        if (toCard.getStatus() == Card.Status.BLOCKED) {
            throw new CardBlockedException("Destination card is blocked");
        }
        if (fromCard.isExpired()) {
            throw new CardExpiredException("Source card is expired");
        }
        if (toCard.isExpired()) {
            throw new CardExpiredException("Destination card is expired");
        }

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        if (fromCard.getId().equals(toCard.getId())) {
            throw new InvalidTransferException("Cannot transfer to the same card");
        }

        Transfer transfer = Transfer.builder()
                .fromCardId(fromCard.getId())
                .toCardId(toCard.getId())
                .amount(request.getAmount())
                .currency(fromCard.getCurrency())
                .description(request.getDescription())
                .status(Transfer.Status.PENDING)
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        try {
            fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
            toCard.setBalance(toCard.getBalance().add(request.getAmount()));

            cardRepository.save(fromCard);
            cardRepository.save(toCard);

            savedTransfer.setStatus(Transfer.Status.COMPLETED);
            transferRepository.save(savedTransfer);

        } catch (OptimisticLockingFailureException e) {
            savedTransfer.setStatus(Transfer.Status.FAILED);
            savedTransfer.setErrorMessage("Concurrent modification detected");
            transferRepository.save(savedTransfer);
            throw new OptimisticLockException("Transfer failed due to concurrent modification");
        }

        return mapToDto(savedTransfer, fromCard, toCard);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getUserTransfers(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Page<Transfer> page = transferRepository.findByUserId(user.getId(), pageable);
        return PageResponse.fromPage(page.map(transfer -> {
            Card fromCard = cardRepository.findById(transfer.getFromCardId()).orElse(null);
            Card toCard = cardRepository.findById(transfer.getToCardId()).orElse(null);
            return mapToDto(transfer, fromCard, toCard);
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getCardTransfers(Long cardId, Pageable pageable) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException("Card not found with id: " + cardId);
        }

        Page<Transfer> page = transferRepository.findByCardId(cardId, pageable);
        return PageResponse.fromPage(page.map(transfer -> {
            Card fromCard = cardRepository.findById(transfer.getFromCardId()).orElse(null);
            Card toCard = cardRepository.findById(transfer.getToCardId()).orElse(null);
            return mapToDto(transfer, fromCard, toCard);
        }));
    }

    private TransferResponse mapToDto(Transfer transfer, Card fromCard, Card toCard) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .fromCardMasked(fromCard != null ? fromCard.getMaskedNumber() : "unknown")
                .toCardMasked(toCard != null ? toCard.getMaskedNumber() : "unknown")
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .status(transfer.getStatus())
                .description(transfer.getDescription())
                .createdAt(transfer.getCreatedAt())
                .errorMessage(transfer.getErrorMessage())
                .build();
    }
}