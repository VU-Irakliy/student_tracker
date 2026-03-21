package com.studio.app.support;

import com.studio.app.enums.Currency;
import com.studio.app.service.CurrencyConversionService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@TestConfiguration
public class StubCurrencyTestConfig {

    @Bean
    @Primary
    public CurrencyConversionService stubCurrencyConversionService() {
        return (amount, sourceCurrency) -> {
            if (amount == null || sourceCurrency == null) {
                return Collections.emptyMap();
            }

            Map<Currency, BigDecimal> eurRates = Map.of(
                    Currency.EUROS, BigDecimal.ONE,
                    Currency.DOLLARS, new BigDecimal("1.09"),
                    Currency.RUBLES, new BigDecimal("101.00")
            );

            BigDecimal sourceToEur = switch (sourceCurrency) {
                case EUROS -> BigDecimal.ONE;
                case DOLLARS -> BigDecimal.ONE.divide(new BigDecimal("1.09"), 10, RoundingMode.HALF_UP);
                case RUBLES -> BigDecimal.ONE.divide(new BigDecimal("101.00"), 10, RoundingMode.HALF_UP);
            };
            BigDecimal amountInEur = amount.multiply(sourceToEur);

            Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
            for (Currency target : Currency.values()) {
                BigDecimal rate = eurRates.get(target);
                result.put(target, amountInEur.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            }
            return Map.copyOf(result);
        };
    }
}

