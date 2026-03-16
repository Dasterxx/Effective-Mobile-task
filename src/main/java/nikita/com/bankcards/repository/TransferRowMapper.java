package nikita.com.bankcards.repository;

import nikita.com.bankcards.entity.Transfer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TransferRowMapper implements RowMapper<Transfer> {

    @Override
    public Transfer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Transfer.builder()
                .id(rs.getLong("id"))
                .fromCardId(rs.getLong("from_card_id"))
                .toCardId(rs.getLong("to_card_id"))
                .amount(rs.getBigDecimal("amount"))
                .currency(rs.getString("currency"))
                .description(rs.getString("description"))
                .status(Transfer.Status.valueOf(rs.getString("status")))
                .errorMessage(rs.getString("error_message"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }
}
