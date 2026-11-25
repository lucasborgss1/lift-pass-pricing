package dojo.liftpasspricing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

/**
 * Implementação de {@link PriceDatabase} com JDBC. Cada operação abre e fecha a própria
 * conexão, em vez de todas as requisições dividirem uma conexão global.
 */
public class JdbcPriceDatabase implements PriceDatabase {

    private final Database database;

    public JdbcPriceDatabase(Database database) {
        this.database = database;
    }

    @Override
    public Optional<Integer> findBaseCost(String type) {
        try (Connection connection = database.connect();
                PreparedStatement stmt = connection.prepareStatement("SELECT cost FROM base_price WHERE type = ?")) {
            stmt.setString(1, type);
            try (ResultSet result = stmt.executeQuery()) {
                return result.next() ? Optional.of(result.getInt("cost")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("erro ao buscar o preço base de " + type, e);
        }
    }

    @Override
    public void saveBaseCost(String type, int cost) {
        try (Connection connection = database.connect();
                PreparedStatement stmt = connection.prepareStatement( //
                        "INSERT INTO base_price (type, cost) VALUES (?, ?) " + //
                        "ON DUPLICATE KEY UPDATE cost = ?")) {
            stmt.setString(1, type);
            stmt.setInt(2, cost);
            stmt.setInt(3, cost);
            stmt.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("erro ao gravar o preço base de " + type, e);
        }
    }

    @Override
    public boolean isHoliday(Date date) {
        try (Connection connection = database.connect();
                PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM holidays WHERE holiday = ?")) {
            stmt.setDate(1, new java.sql.Date(date.getTime()));
            try (ResultSet result = stmt.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("erro ao consultar os feriados", e);
        }
    }
}
