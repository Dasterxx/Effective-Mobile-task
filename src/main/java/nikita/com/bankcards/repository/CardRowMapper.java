package nikita.com.bankcards.repository;

import nikita.com.bankcards.entity.Card;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CardRowMapper implements RowMapper<Card> {

    @Override
    public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Card.builder()
                .id(rs.getLong("id"))
                .cardNumber(rs.getString("card_number"))
                .maskedNumber(rs.getString("masked_number"))
                .ownerName(rs.getString("owner_name"))
                .expiryDate(rs.getDate("expiry_date").toLocalDate())
                .status(Card.Status.valueOf(rs.getString("status")))
                .balance(rs.getBigDecimal("balance"))
                .currency(rs.getString("currency"))
                .ownerId(rs.getLong("owner_id"))
                .version(rs.getLong("version"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }
}