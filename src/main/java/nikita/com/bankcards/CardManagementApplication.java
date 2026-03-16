package nikita.com.bankcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CardManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardManagementApplication.class, args);
    }
}
