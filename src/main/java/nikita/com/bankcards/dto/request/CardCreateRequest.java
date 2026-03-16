package nikita.com.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@RequiredArgsConstructor
@Getter
@Setter
public class CardCreateRequest {

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter code")
    private String currency = "RUB";
}
