package nikita.com.bankcards.service;


import nikita.com.bankcards.dto.UserDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface UserService {

    CompletableFuture<UserDto> getUserByUsername(String username);

    CompletableFuture<UserDto> getUserById(Long id);

    CompletableFuture<List<UserDto>> getAllUsers();

    CompletableFuture<Void> deleteUser(Long id);
}
