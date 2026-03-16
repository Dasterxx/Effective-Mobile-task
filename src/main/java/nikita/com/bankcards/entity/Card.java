package nikita.com.bankcards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false)
    private String cardNumber; // Зашифрованное хранение

    @Column(name = "masked_number", nullable = false)
    private String maskedNumber; // **** **** **** 1234

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    private String currency;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Version
    private Long version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        ACTIVE, BLOCKED, EXPIRED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == Status.ACTIVE && expiryDate.isAfter(LocalDate.now());
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now()) || status == Status.EXPIRED;
    }
}