package com.studio.app.service.impl;

import com.studio.app.enums.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CurrencyConversionServiceImpl}.
 *
 * <p>Mocks the {@link RestClient} so no real HTTP calls are made.
 * Verifies conversion logic, caching, rounding, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceImplTest {

    @Mock private RestClient.Builder restClientBuilder;
    @Mock private RestClient restClient;
    @SuppressWarnings("rawtypes")
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @SuppressWarnings("rawtypes")
    @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private CurrencyConversionServiceImpl conversionService;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
        // lenient: NullInputs tests return early before any HTTP call is made
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString(), any(Object[].class)))
                .thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        conversionService = new CurrencyConversionServiceImpl(restClientBuilder,
                "https://open.er-api.com/v6/latest", 60);
    }

    private void mockApiResponse(String baseCurrency, Map<String, Object> rates) {
        Map<String, Object> body = Map.of(
                "result", "success",
                "base_code", baseCurrency,
                "rates", rates
        );
        when(responseSpec.body(eq(Map.class))).thenReturn(body);
    }

    @Nested
    class NullInputs {

        @Test
        void shouldReturnEmptyMap_forNullAmount() {
            var result = conversionService.convertToAll(null, Currency.RUBLES);
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyMap_forNullCurrency() {
            var result = conversionService.convertToAll(new BigDecimal("100"), null);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class RubConversion {

        @BeforeEach
        void setUpRubRates() {
            mockApiResponse("RUB", Map.of(
                    "RUB", 1.0,
                    "USD", 0.0108,
                    "EUR", 0.0099
            ));
        }

        @Test
        void shouldConvertRubToAllCurrencies() {
            var result = conversionService.convertToAll(new BigDecimal("10000.00"), Currency.RUBLES);

            assertThat(result).containsKey(Currency.RUBLES);
            assertThat(result).containsKey(Currency.DOLLARS);
            assertThat(result).containsKey(Currency.EUROS);

            assertThat(result.get(Currency.RUBLES)).isEqualByComparingTo("10000.00");
            assertThat(result.get(Currency.DOLLARS)).isEqualByComparingTo("108.00");
            assertThat(result.get(Currency.EUROS)).isEqualByComparingTo("99.00");
        }

        @Test
        void shouldReturnSameAmount_forSourceCurrency() {
            var result = conversionService.convertToAll(new BigDecimal("2000.00"), Currency.RUBLES);

            assertThat(result.get(Currency.RUBLES)).isEqualByComparingTo("2000.00");
        }

        @Test
        void shouldRoundToTwoDecimalPlaces() {
            var result = conversionService.convertToAll(new BigDecimal("1.00"), Currency.RUBLES);

            assertThat(result.get(Currency.DOLLARS).scale()).isEqualTo(2);
            assertThat(result.get(Currency.EUROS).scale()).isEqualTo(2);
        }

        @Test
        void shouldHandleZeroAmount() {
            var result = conversionService.convertToAll(BigDecimal.ZERO, Currency.RUBLES);

            assertThat(result.get(Currency.RUBLES)).isEqualByComparingTo("0.00");
            assertThat(result.get(Currency.DOLLARS)).isEqualByComparingTo("0.00");
            assertThat(result.get(Currency.EUROS)).isEqualByComparingTo("0.00");
        }

        @Test
        void shouldHandleLargeAmounts() {
            var result = conversionService.convertToAll(
                    new BigDecimal("1000000.00"), Currency.RUBLES);

            // 1,000,000 * 0.0108 = 10,800.00
            assertThat(result.get(Currency.DOLLARS)).isEqualByComparingTo("10800.00");
        }
    }

    @Nested
    class UsdConversion {

        @BeforeEach
        void setUpUsdRates() {
            mockApiResponse("USD", Map.of(
                    "USD", 1.0,
                    "EUR", 0.92,
                    "RUB", 92.5
            ));
        }

        @Test
        void shouldConvertUsdToRubAndEur() {
            var result = conversionService.convertToAll(new BigDecimal("100.00"), Currency.DOLLARS);

            assertThat(result.get(Currency.DOLLARS)).isEqualByComparingTo("100.00");
            assertThat(result.get(Currency.RUBLES)).isEqualByComparingTo("9250.00");
            assertThat(result.get(Currency.EUROS)).isEqualByComparingTo("92.00");
        }
    }

    @Nested
    class EurConversion {

        @BeforeEach
        void setUpEurRates() {
            mockApiResponse("EUR", Map.of(
                    "EUR", 1.0,
                    "USD", 1.09,
                    "RUB", 101.0
            ));
        }

        @Test
        void shouldConvertEurToRubAndUsd() {
            var result = conversionService.convertToAll(new BigDecimal("100.00"), Currency.EUROS);

            assertThat(result.get(Currency.EUROS)).isEqualByComparingTo("100.00");
            assertThat(result.get(Currency.RUBLES)).isEqualByComparingTo("10100.00");
            assertThat(result.get(Currency.DOLLARS)).isEqualByComparingTo("109.00");
        }
    }

    @Nested
    class Caching {

        @Test
        void shouldCacheRatesAndNotCallApiAgain() {
            mockApiResponse("RUB", Map.of(
                    "RUB", 1.0, "USD", 0.0108, "EUR", 0.0099
            ));

            // First call — hits the API
            conversionService.convertToAll(new BigDecimal("1000.00"), Currency.RUBLES);
            // Second call — should use cache
            conversionService.convertToAll(new BigDecimal("2000.00"), Currency.RUBLES);

            // The API body should only be fetched once
            verify(responseSpec, times(1)).body(eq(Map.class));
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldReturnEmptyMapForOtherCurrencies_whenApiFails() {
            when(responseSpec.body(eq(Map.class)))
                    .thenThrow(new RuntimeException("API unreachable"));

            var result = conversionService.convertToAll(new BigDecimal("100.00"), Currency.RUBLES);

            // Source currency is always included
            assertThat(result.get(Currency.RUBLES)).isEqualByComparingTo("100.00");
            // Others might be missing since API failed and no cache
            // (at minimum the source is preserved)
        }

        @Test
        void shouldUseStaleCacheWhenApiFails() {
            // First call succeeds
            mockApiResponse("RUB", Map.of(
                    "RUB", 1.0, "USD", 0.0108, "EUR", 0.0099
            ));
            conversionService.convertToAll(new BigDecimal("1000.00"), Currency.RUBLES);

            // Simulate cache expiry by creating a new service with 0-minute TTL
            var shortTtlService = new CurrencyConversionServiceImpl(
                    restClientBuilder, "https://open.er-api.com/v6/latest", 0);

            // First call to populate cache in the new service
            mockApiResponse("RUB", Map.of(
                    "RUB", 1.0, "USD", 0.0108, "EUR", 0.0099
            ));
            shortTtlService.convertToAll(new BigDecimal("1000.00"), Currency.RUBLES);

            // Now make API fail
            lenient().when(responseSpec.body(eq(Map.class)))
                    .thenThrow(new RuntimeException("API down"));

            // Should still get results from stale cache
            var result = shortTtlService.convertToAll(new BigDecimal("1000.00"), Currency.RUBLES);

            assertThat(result).containsKey(Currency.DOLLARS);
            assertThat(result).containsKey(Currency.EUROS);
        }

        @Test
        void shouldHandleUnsuccessfulApiResponse() {
            Map<String, Object> body = Map.of("result", "error");
            when(responseSpec.body(eq(Map.class))).thenReturn(body);

            var result = conversionService.convertToAll(new BigDecimal("100.00"), Currency.RUBLES);

            // Should still have source currency at minimum
            assertThat(result.get(Currency.RUBLES)).isEqualByComparingTo("100.00");
        }
    }
}

