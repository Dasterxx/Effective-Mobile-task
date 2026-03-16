package nikita.com.bankcards.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class AuthResponse {
    private String token;
    private String type;
    private Long userId;
    private String username;
    private String role;
}
