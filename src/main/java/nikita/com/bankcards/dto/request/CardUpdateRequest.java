package nikita.com.bankcards.dto.request;

import lombok.ToString;
import nikita.com.bankcards.entity.Card;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class CardUpdateRequest {
    private Card.Status status;
    private String ownerName;
}
