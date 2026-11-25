package dojo.liftpasspricing;

import java.util.Date;
import java.util.Optional;

/**
 * Tudo o que a regra de preço precisa do banco. Com essa interface a regra pode ser
 * testada com uma implementação em memória, sem JDBC.
 */
public interface PriceDatabase {

    /** Preço base do tipo de passe, ou vazio se o tipo não estiver cadastrado. */
    Optional<Integer> findBaseCost(String type);

    /** Cadastra o tipo de passe ou altera o preço base dele. */
    void saveBaseCost(String type, int cost);

    boolean isHoliday(Date date);
}
