package nikita.com.bankcards.service.user;

import lombok.RequiredArgsConstructor;
import nikita.com.bankcards.dto.UserDto;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.exception.user.UserNotFoundException;
import nikita.com.bankcards.repository.UserRepository;
import nikita.com.bankcards.service.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<UserDto> getUserByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
            return mapToDto(user);
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<UserDto> getUserById(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
            return mapToDto(user);
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<UserDto>> getAllUsers() {
        return CompletableFuture.supplyAsync(() ->
                userRepository.findAll().stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList())
        );
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Void> deleteUser(Long id) {
        return CompletableFuture.runAsync(() -> {
            if (!userRepository.existsById(id)) {
                throw new UserNotFoundException("User not found with id: " + id);
            }
            userRepository.deleteById(id);
        });
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}