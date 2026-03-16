package nikita.com.bankcards.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nikita.com.bankcards.entity.User;
import nikita.com.bankcards.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Transactional
    public CommandLineRunner recreateAdmin() {
        return args -> {
            String adminUsername = "admin";
            String adminPassword = "admin123";

            // 1. Принудительно удаляем админа (даже если есть constraint)
            userRepository.findByUsername(adminUsername).ifPresent(existingAdmin -> {
                log.info("Deleting existing admin (id={})", existingAdmin.getId());
                userRepository.delete(existingAdmin);
                userRepository.flush(); // Принудительный flush
                log.info("Admin deleted successfully");
            });

            // 2. Двойная проверка - если всё ещё есть (например, из-за кэша)
            if (userRepository.findByUsername(adminUsername).isPresent()) {
                log.warn("Admin still exists after delete! Forcing native delete...");
                userRepository.deleteByUsername(adminUsername);
            }

            // 3. Создаём нового админа с правильным хешем
            String hashedPassword = passwordEncoder.encode(adminPassword);

            User admin = User.builder()
                    .username(adminUsername)
                    .password(hashedPassword)
                    .email("admin@bank.com")
                    .role(User.Role.ADMIN)
                    .build();

            User saved = userRepository.save(admin);

            log.info("========================================");
            log.info("ADMIN RECREATED SUCCESSFULLY");
            log.info("ID: {}", saved.getId());
            log.info("Username: {}", adminUsername);
            log.info("Password: {}", adminPassword);
            log.info("Hash: {}", hashedPassword);
            log.info("========================================");
        };
    }
}