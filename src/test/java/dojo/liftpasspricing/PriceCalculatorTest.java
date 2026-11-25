package dojo.liftpasspricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Testa a regra de preço direto, sem servidor nem banco de verdade.
 * 2019-02-11 é segunda, 2019-02-18 é segunda e feriado, 2019-02-14 é quinta.
 */
class PriceCalculatorTest {

    private static final String MONDAY = "2019-02-11";
    private static final String HOLIDAY_MONDAY = "2019-02-18";
    private static final String THURSDAY = "2019-02-14";

    private final FakePriceDatabase database = new FakePriceDatabase()
            .withBaseCost("1jour", 35)
            .withBaseCost("night", 19)
            .withHoliday(HOLIDAY_MONDAY);
    private final PriceCalculator calculator = new PriceCalculator(database);

    @Test
    void childrenUnderSixDoNotPay() throws Exception {
        assertEquals(0, price("1jour", 5, MONDAY));
        assertEquals(0, price("night", 5, MONDAY));
    }

    @Test
    void adultPaysFullPriceOnRegularDay() throws Exception {
        assertEquals(35, price("1jour", 30, THURSDAY));
    }

    @Test
    void mondayGivesThirtyFivePercentOff() throws Exception {
        assertEquals(23, price("1jour", 30, MONDAY));
    }

    @Test
    void mondayHolidayHasNoReduction() throws Exception {
        assertEquals(35, price("1jour", 30, HOLIDAY_MONDAY));
    }

    @Test
    void childPaysSeventyPercentAndGetsNoMondayReduction() throws Exception {
        assertEquals(25, price("1jour", 10, THURSDAY));
        assertEquals(25, price("1jour", 10, MONDAY));
    }

    @Test
    void seniorPaysSeventyFivePercent() throws Exception {
        assertEquals(27, price("1jour", 65, THURSDAY));
    }

    @Test
    void seniorAlsoGetsMondayReduction() throws Exception {
        assertEquals(18, price("1jour", 65, MONDAY));
    }

    @Test
    void missingAgeIsPricedAsAdult() throws Exception {
        assertEquals(23, price("1jour", null, MONDAY));
        assertEquals(35, price("1jour", null, null));
    }

    @Test
    void nightPassForAdultCostsBasePriceEvenOnMonday() throws Exception {
        assertEquals(19, price("night", 30, MONDAY));
    }

    @Test
    void nightPassForSeniorCostsFortyPercent() throws Exception {
        assertEquals(8, price("night", 65, THURSDAY));
    }

    @Test
    void nightPassWithoutAgeIsFree() throws Exception {
        assertEquals(0, price("night", null, THURSDAY));
    }

    @Test
    void otherRegisteredTypesArePricedAsDayPass() throws Exception {
        database.withBaseCost("semana", 50);
        assertEquals(33, price("semana", 30, MONDAY));
    }

    private int price(String type, Integer age, String date) throws Exception {
        return calculator.calculate(type, age, date);
    }
}
