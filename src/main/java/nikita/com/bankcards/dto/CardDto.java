package nikita.com.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nikita.com.bankcards.entity.Card;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDto {
    private Long id;
    private String maskedNumber;
    private Long ownerId;
    private LocalDate expiryDate;
    private Card.Status status;
    private BigDecimal balance;
    private String currency;

    public static CardDto fromEntity(Card card) {
        return CardDto.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .ownerId(card.getOwnerId())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .currency(card.getCurrency())
                .build();
    }
}
