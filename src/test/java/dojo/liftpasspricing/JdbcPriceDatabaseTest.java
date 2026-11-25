package dojo.liftpasspricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Testa o SQL contra um H2 próprio, separado do banco usado pelo servidor de teste. */
class JdbcPriceDatabaseTest {

    private static JdbcPriceDatabase database;

    @BeforeAll
    static void createDatabase() throws Exception {
        Database h2 = new Database("jdbc:h2:mem:jdbc_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        h2.createSchemaIfEmbedded();
        database = new JdbcPriceDatabase(h2);
    }

    @Test
    void findsBaseCostOfExistingType() {
        assertEquals(Optional.of(19), database.findBaseCost("night"));
    }

    @Test
    void returnsEmptyForUnknownType() {
        assertEquals(Optional.empty(), database.findBaseCost("vip"));
    }

    @Test
    void insertsAndThenUpdatesBaseCost() {
        database.saveBaseCost("semana", 50);
        assertEquals(Optional.of(50), database.findBaseCost("semana"));

        database.saveBaseCost("semana", 55);
        assertEquals(Optional.of(55), database.findBaseCost("semana"));
    }

    @Test
    void recognizesHolidays() throws Exception {
        assertTrue(database.isHoliday(date("2019-02-18")));
        assertFalse(database.isHoliday(date("2019-02-11")));
    }

    private static Date date(String isoDate) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(isoDate);
    }
}
