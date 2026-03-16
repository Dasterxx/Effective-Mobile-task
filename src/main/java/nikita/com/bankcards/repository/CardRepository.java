package nikita.com.bankcards.repository;

import nikita.com.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.cardNumber = :cardNumber")
    Optional<Card> findByCardNumberWithLock(@Param("cardNumber") String cardNumber);

    Optional<Card> findByCardNumber(String cardNumber);

    // Новый метод для поиска по maskedNumber
    Optional<Card> findByMaskedNumber(String maskedNumber);

    // Заменяем findByOwner на findByOwnerId
    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Card> findByOwnerIdAndStatus(Long ownerId, Card.Status status, Pageable pageable);

    // Новый метод для поиска по статусу (все карты)
    Page<Card> findByStatus(Card.Status status, Pageable pageable);

    boolean existsByCardNumber(String cardNumber);

    @Query("SELECT c.balance FROM Card c WHERE c.id = :id")
    Optional<java.math.BigDecimal> findBalanceById(@Param("id") Long id);

    @Query("SELECT c FROM Card c WHERE c.ownerId = :ownerId AND c.status = 'ACTIVE'")
    java.util.List<Card> findActiveCardsByOwnerId(@Param("ownerId") Long ownerId);
}