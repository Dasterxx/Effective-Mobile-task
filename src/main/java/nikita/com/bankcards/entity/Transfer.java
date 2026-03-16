package nikita.com.bankcards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_card_id", nullable = false)
    private Long fromCardId;

    @Column(name = "to_card_id", nullable = false)
    private Long toCardId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String currency;

    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private Card fromCard;

    @Transient
    private Card toCard;

    public enum Status {
        PENDING, COMPLETED, FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}