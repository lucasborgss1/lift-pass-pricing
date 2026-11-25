package dojo.liftpasspricing;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/** Regra de preço do passe, separada do HTTP. */
public class PriceCalculator {

    private final PriceDatabase database;

    public PriceCalculator(PriceDatabase database) {
        this.database = database;
    }

    /**
     * Calcula o preço do passe.
     *
     * @param type tipo de passe, por exemplo {@code 1jour} ou {@code night}
     * @param age idade, ou {@code null} se não foi informada
     * @param date data no formato {@code yyyy-MM-dd}, ou {@code null}
     */
    public int calculate(String type, Integer age, String date) throws ParseException {
        // null quando o tipo não existe; o erro só aparece quando o valor é usado, como no legado
        Integer baseCost = database.findBaseCost(type).orElse(null);

        int reduction;
        boolean isHoliday = false;

        if (age != null && age < 6) {
            return 0;
        } else {
            reduction = 0;

            if (!type.equals("night")) {
                DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

                if (date != null) {
                    Date d = isoFormat.parse(date);
                    isHoliday = database.isHoliday(d);
                }

                if (date != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(isoFormat.parse(date));
                    if (!isHoliday && calendar.get(Calendar.DAY_OF_WEEK) == 2) {
                        reduction = 35;
                    }
                }

                // TODO apply reduction for others
                if (age != null && age < 15) {
                    return (int) Math.ceil(baseCost * .7);
                } else {
                    if (age == null) {
                        double cost = baseCost * (1 - reduction / 100.0);
                        return (int) Math.ceil(cost);
                    } else {
                        if (age > 64) {
                            double cost = baseCost * .75 * (1 - reduction / 100.0);
                            return (int) Math.ceil(cost);
                        } else {
                            double cost = baseCost * (1 - reduction / 100.0);
                            return (int) Math.ceil(cost);
                        }
                    }
                }
            } else {
                if (age != null && age >= 6) {
                    if (age > 64) {
                        return (int) Math.ceil(baseCost * .4);
                    } else {
                        return baseCost;
                    }
                } else {
                    return 0;
                }
            }
        }
    }
}
