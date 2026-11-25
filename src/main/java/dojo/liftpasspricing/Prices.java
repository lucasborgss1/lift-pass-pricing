package dojo.liftpasspricing;

import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.put;

import java.sql.SQLException;

public class Prices {

    public static void createApp() throws SQLException {

        Database connectionSource = Database.fromSystemProperty();
        connectionSource.createSchemaIfEmbedded();
        final PriceDatabase database = new JdbcPriceDatabase(connectionSource);
        final PriceCalculator calculator = new PriceCalculator(database);

        port(4567);

        put("/prices", (req, res) -> {
            int liftPassCost = Integer.parseInt(req.queryParams("cost"));
            String liftPassType = req.queryParams("type");

            database.saveBaseCost(liftPassType, liftPassCost);

            return "";
        });

        get("/prices", (req, res) -> {
            final Integer age = req.queryParams("age") != null ? Integer.valueOf(req.queryParams("age")) : null;

            int cost = calculator.calculate(req.queryParams("type"), age, req.queryParams("date"));
            return "{ \"cost\": " + cost + "}";
        });

        after((req, res) -> {
            res.type("application/json");
        });
    }

}
