package dojo.liftpasspricing;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Banco em memória para testar a regra de preço sem JDBC. */
class FakePriceDatabase implements PriceDatabase {

    private final Map<String, Integer> baseCosts = new HashMap<>();
    private final Set<String> holidays = new HashSet<>();

    FakePriceDatabase withBaseCost(String type, int cost) {
        baseCosts.put(type, cost);
        return this;
    }

    FakePriceDatabase withHoliday(String isoDate) {
        holidays.add(isoDate);
        return this;
    }

    @Override
    public Optional<Integer> findBaseCost(String type) {
        return Optional.ofNullable(baseCosts.get(type));
    }

    @Override
    public void saveBaseCost(String type, int cost) {
        baseCosts.put(type, cost);
    }

    @Override
    public boolean isHoliday(Date date) {
        return holidays.contains(new SimpleDateFormat("yyyy-MM-dd").format(date));
    }
}
