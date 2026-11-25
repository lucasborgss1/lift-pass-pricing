package dojo.liftpasspricing;

import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.put;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Prices {

    public static void createApp() throws SQLException {

        Database connectionSource = Database.fromSystemProperty();
        connectionSource.createSchemaIfEmbedded();
        final PriceDatabase database = new JdbcPriceDatabase(connectionSource);

        port(4567);

        put("/prices", (req, res) -> {
            int liftPassCost = Integer.parseInt(req.queryParams("cost"));
            String liftPassType = req.queryParams("type");

            database.saveBaseCost(liftPassType, liftPassCost);

            return "";
        });

        get("/prices", (req, res) -> {
            final Integer age = req.queryParams("age") != null ? Integer.valueOf(req.queryParams("age")) : null;

            // null quando o tipo não existe; o erro só aparece quando o valor é usado, como no legado
            Integer baseCost = database.findBaseCost(req.queryParams("type")).orElse(null);

            int reduction;
            boolean isHoliday = false;

            if (age != null && age < 6) {
                return "{ \"cost\": 0}";
            } else {
                reduction = 0;

                if (!req.queryParams("type").equals("night")) {
                    DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

                    if (req.queryParams("date") != null) {
                        Date d = isoFormat.parse(req.queryParams("date"));
                        isHoliday = database.isHoliday(d);
                    }

                    if (req.queryParams("date") != null) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(isoFormat.parse(req.queryParams("date")));
                        if (!isHoliday && calendar.get(Calendar.DAY_OF_WEEK) == 2) {
                            reduction = 35;
                        }
                    }

                    // TODO apply reduction for others
                    if (age != null && age < 15) {
                        return "{ \"cost\": " + (int) Math.ceil(baseCost * .7) + "}";
                    } else {
                        if (age == null) {
                            double cost = baseCost * (1 - reduction / 100.0);
                            return "{ \"cost\": " + (int) Math.ceil(cost) + "}";
                        } else {
                            if (age > 64) {
                                double cost = baseCost * .75 * (1 - reduction / 100.0);
                                return "{ \"cost\": " + (int) Math.ceil(cost) + "}";
                            } else {
                                double cost = baseCost * (1 - reduction / 100.0);
                                return "{ \"cost\": " + (int) Math.ceil(cost) + "}";
                            }
                        }
                    }
                } else {
                    if (age != null && age >= 6) {
                        if (age > 64) {
                            return "{ \"cost\": " + (int) Math.ceil(baseCost * .4) + "}";
                        } else {
                            return "{ \"cost\": " + baseCost + "}";
                        }
                    } else {
                        return "{ \"cost\": 0}";
                    }
                }
            }
        });

        after((req, res) -> {
            res.type("application/json");
        });
    }

}
