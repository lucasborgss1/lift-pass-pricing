package dojo.liftpasspricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Registra o que o GET /prices responde hoje, inclusive nos casos estranhos, para garantir
 * que a refatoração não muda nenhum preço.
 * Base: 1jour = 35 e night = 19. 2019-02-11 é segunda, 2019-02-18 é segunda e feriado,
 * 2019-02-14 é quinta. Campo vazio no CSV significa parâmetro ausente.
 */
class PricesCharacterizationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void startServer() throws Exception {
        TestServer.start();
    }

    @ParameterizedTest(name = "type={0}, age={1}, date={2} -> {3}")
    @CsvSource({
            "1jour,   , 2019-02-11, 23",
            "1jour,  5, 2019-02-11,  0",
            "1jour,  6, 2019-02-11, 25",
            "1jour, 14, 2019-02-11, 25",
            "1jour, 15, 2019-02-11, 23",
            "1jour, 64, 2019-02-11, 23",
            "1jour, 65, 2019-02-11, 18",
            "1jour,   , 2019-02-18, 35",
            "1jour, 14, 2019-02-18, 25",
            "1jour, 65, 2019-02-18, 27",
            "1jour, 15, 2019-02-14, 35",
            "1jour, 65, 2019-02-14, 27",
            "1jour,   ,           , 35",
            "night,   , 2019-02-11,  0",
            "night,  5, 2019-02-11,  0",
            "night,  6, 2019-02-11, 19",
            "night, 64, 2019-02-11, 19",
            "night, 65, 2019-02-11,  8",
    })
    void keepsLegacyPrice(String type, Integer age, String date, int expectedCost) throws Exception {
        String query = "/prices?type=" + type
                + (age != null ? "&age=" + age : "")
                + (date != null ? "&date=" + date : "");

        HttpResponse<String> response = TestServer.get(query);

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals(expectedCost, JSON.readTree(response.body()).get("cost").asInt());
    }
}
