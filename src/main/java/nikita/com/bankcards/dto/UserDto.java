package nikita.com.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nikita.com.bankcards.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private User.Role role;
    private LocalDateTime createdAt;
    private List<CardDto> cards;

    public static UserDto fromEntity(User user, boolean includeCards) {
        UserDtoBuilder builder = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt());

        if (includeCards && user.getCards() != null) {
            builder.cards(user.getCards().stream()
                    .map(CardDto::fromEntity)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
