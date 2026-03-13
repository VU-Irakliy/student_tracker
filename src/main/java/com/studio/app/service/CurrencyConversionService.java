package com.studio.app.service;

import com.studio.app.enums.Currency;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Converts a monetary amount from one {@link Currency} to all supported currencies.
 */
public interface CurrencyConversionService {

    /**
     * Converts the given amount from {@code sourceCurrency} into every
     * supported {@link Currency}.
     *
     * @param amount         the original amount
     * @param sourceCurrency the currency of {@code amount}
     * @return an unmodifiable map keyed by target currency with converted values
     */
    Map<Currency, BigDecimal> convertToAll(BigDecimal amount, Currency sourceCurrency);
}
