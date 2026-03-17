package nikita.com.bankcards.service;

import nikita.com.bankcards.dto.UserDto;
import java.util.List;

public interface UserService {
    UserDto getUserByUsername(String username);
    UserDto getUserById(Long id);
    List<UserDto> getAllUsers();
    void deleteUser(Long id);
}