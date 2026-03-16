package nikita.com.bankcards.repository;

import nikita.com.bankcards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByFromCardIdOrToCardIdOrderByCreatedAtDesc(Long fromCardId, Long toCardId);

    @Query("SELECT t FROM Transfer t WHERE t.fromCardId = :cardId OR t.toCardId = :cardId ORDER BY t.createdAt DESC")
    Page<Transfer> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

    @Query(value = """
        SELECT t.* FROM transfers t 
        WHERE t.from_card_id IN (SELECT id FROM cards WHERE owner_id = :userId) 
           OR t.to_card_id IN (SELECT id FROM cards WHERE owner_id = :userId) 
        ORDER BY t.created_at DESC
        """, nativeQuery = true)
    Page<Transfer> findByUserId(@Param("userId") Long userId, Pageable pageable);
}