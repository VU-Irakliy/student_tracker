package com.studio.app.service.impl;

import com.studio.app.enums.Currency;
import com.studio.app.service.CurrencyConversionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts amounts between supported currencies using the
 * <a href="https://open.er-api.com">Open Exchange Rate API</a>.
 *
 * <p>This free REST API supports all major currencies including RUB, requires
 * no API key, and is rate-limited to ~1500 requests/month on the free tier.
 *
 * <p>To stay well within limits the service caches exchange rates per base
 * currency for a configurable duration ({@code currency.cache-ttl-minutes},
 * default 60 minutes).  The cache is a simple in-memory
 * {@link ConcurrentHashMap} — no external dependency needed.
 */
@Slf4j
@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final RestClient restClient;
    private final Duration cacheTtl;

    /** Cached rate maps keyed by source ISO code (e.g. "USD"). */
    private final ConcurrentHashMap<String, CachedRates> cache = new ConcurrentHashMap<>();

    public CurrencyConversionServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${currency.api.base-url:https://open.er-api.com/v6/latest}") String baseUrl,
            @Value("${currency.cache-ttl-minutes:60}") long cacheTtlMinutes) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
    }

    @Override
    public Map<Currency, BigDecimal> convertToAll(BigDecimal amount, Currency sourceCurrency) {
        if (amount == null || sourceCurrency == null) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> rates = getRates(sourceCurrency.getIsoCode());
        Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);

        for (Currency target : Currency.values()) {
            if (target == sourceCurrency) {
                result.put(target, amount.setScale(2, RoundingMode.HALF_UP));
                continue;
            }
            BigDecimal rate = rates.get(target.getIsoCode());
            if (rate != null) {
                result.put(target, amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            } else {
                log.warn("No exchange rate found for {} → {}", sourceCurrency, target);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    // ── rate fetching & caching ────────────────────────────────────────────

    /**
     * Returns exchange rates for the given base currency ISO code.
     * Uses a cached value if still fresh, otherwise fetches from the API.
     */
    private Map<String, BigDecimal> getRates(String baseIso) {
        CachedRates cached = cache.get(baseIso);
        if (cached != null && !cached.isExpired(cacheTtl)) {
            return cached.rates();
        }

        try {
            Map<String, BigDecimal> fresh = fetchRatesFromApi(baseIso);
            cache.put(baseIso, new CachedRates(Instant.now(), fresh));
            return fresh;
        } catch (Exception e) {
            log.error("Failed to fetch exchange rates for {}: {}", baseIso, e.getMessage());
            // Return stale cache if available, otherwise empty
            if (cached != null) {
                log.warn("Using stale cached rates for {}", baseIso);
                return cached.rates();
            }
            return Collections.emptyMap();
        }
    }

    /**
     * Calls the Open Exchange Rate API and extracts the "rates" map.
     * Response shape: {@code { "result": "success", "rates": { "EUR": 0.92, "RUB": 92.5, ... } }}
     */
    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> fetchRatesFromApi(String baseIso) {
        log.info("Fetching exchange rates from API for base currency: {}", baseIso);

        Map<String, Object> body = restClient.get()
                .uri("/{base}", baseIso)
                .retrieve()
                .body(Map.class);

        if (body == null || !"success".equals(body.get("result"))) {
            throw new IllegalStateException("Exchange rate API returned an unsuccessful response");
        }

        Map<String, Object> rawRates = (Map<String, Object>) body.get("rates");
        if (rawRates == null) {
            throw new IllegalStateException("Exchange rate API response missing 'rates' field");
        }

        Map<String, BigDecimal> parsed = new HashMap<>();
        for (var entry : rawRates.entrySet()) {
            try {
                parsed.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
            } catch (NumberFormatException e) {
                log.warn("Skipping unparseable rate: {} = {}", entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    // ── cache record ───────────────────────────────────────────────────────

    private record CachedRates(Instant fetchedAt, Map<String, BigDecimal> rates) {
        boolean isExpired(Duration ttl) {
            return Instant.now().isAfter(fetchedAt.plus(ttl));
        }
    }
}

