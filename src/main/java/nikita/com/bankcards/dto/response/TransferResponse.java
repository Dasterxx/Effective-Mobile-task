package nikita.com.bankcards.dto.response;

import nikita.com.bankcards.entity.Transfer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class TransferResponse {
    private Long id;
    private String fromCardMasked;
    private String toCardMasked;
    private BigDecimal amount;
    private String currency;
    private Transfer.Status status;
    private String description;
    private LocalDateTime createdAt;
    private String errorMessage;

    public static TransferResponse fromEntity(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .fromCardMasked(transfer.getFromCard().getMaskedNumber())
                .toCardMasked(transfer.getToCard().getMaskedNumber())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .status(transfer.getStatus())
                .description(transfer.getDescription())
                .createdAt(transfer.getCreatedAt())
                .errorMessage(transfer.getErrorMessage())
                .build();
    }
}
