package dojo.liftpasspricing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String DEFAULT_URL = "jdbc:h2:mem:lift_pass;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private final String url;

    public Database(String url) {
        this.url = url;
    }

    /**
     * Usa a URL da system property {@code liftpass.db.url}, ou o H2 em memória se ela não existir.
     */
    public static Database fromSystemProperty() {
        return new Database(System.getProperty("liftpass.db.url", DEFAULT_URL));
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /**
     * Cria as tabelas e os dados de exemplo no H2. Num banco servidor o schema vem de
     * database/initDatabase.sql, então aqui não faz nada.
     */
    public void createSchemaIfEmbedded() throws SQLException {
        if (!url.startsWith("jdbc:h2:")) {
            return;
        }
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("RUNSCRIPT FROM 'classpath:/db/init.sql'");
        }
    }
}
