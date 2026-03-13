package com.studio.app.integration;

import com.studio.app.enums.Currency;
import com.studio.app.service.CurrencyConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Base class for controller integration tests.
 * <ul>
 *   <li>Loads full Spring context with H2 in-memory DB</li>
 *   <li>Inserts test data via {@code data-test.sql} before each test method</li>
 *   <li>Cleans up via {@code cleanup-test.sql} after each test method</li>
 *   <li>Provides a stub {@link CurrencyConversionService} so no real HTTP calls are made</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/data-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;

    /**
     * Replaces the real {@link CurrencyConversionService} with a deterministic
     * stub that uses fixed rates: 1 EUR = 1.09 USD = 101 RUB.
     */
    @TestConfiguration
    static class StubCurrencyConfig {

        @Bean
        @Primary
        public CurrencyConversionService stubCurrencyConversionService() {
            return (amount, sourceCurrency) -> {
                if (amount == null || sourceCurrency == null) {
                    return Collections.emptyMap();
                }
                // Fixed rates relative to EUR: 1 EUR = 1.09 USD = 101 RUB
                Map<Currency, BigDecimal> eurRates = Map.of(
                        Currency.EUROS,   BigDecimal.ONE,
                        Currency.DOLLARS, new BigDecimal("1.09"),
                        Currency.RUBLES,  new BigDecimal("101.00")
                );
                // Convert source → EUR first, then EUR → all targets
                BigDecimal sourceToEur = switch (sourceCurrency) {
                    case EUROS   -> BigDecimal.ONE;
                    case DOLLARS -> BigDecimal.ONE.divide(new BigDecimal("1.09"), 10, RoundingMode.HALF_UP);
                    case RUBLES  -> BigDecimal.ONE.divide(new BigDecimal("101.00"), 10, RoundingMode.HALF_UP);
                };
                BigDecimal amountInEur = amount.multiply(sourceToEur);

                Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
                for (Currency target : Currency.values()) {
                    BigDecimal rate = eurRates.get(target);
                    result.put(target, amountInEur.multiply(rate).setScale(2, RoundingMode.HALF_UP));
                }
                return Collections.unmodifiableMap(result);
            };
        }
    }
}

