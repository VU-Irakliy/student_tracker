package com.studio.app.enums;

/**
 * Supported currencies for student pricing.
 * Each value maps to its ISO 4217 currency code.
 */
public enum Currency {

    DOLLARS("USD"),
    EUROS("EUR"),
    RUBLES("RUB");

    private final String isoCode;

    Currency(String isoCode) {
        this.isoCode = isoCode;
    }

    public String getIsoCode() {
        return isoCode;
    }

    /**
     * Looks up a {@link Currency} by its ISO 4217 code.
     *
     * @param isoCode e.g. "USD", "EUR", "RUB"
     * @return the matching enum constant
     * @throws IllegalArgumentException if no match is found
     */
    public static Currency fromIsoCode(String isoCode) {
        for (Currency c : values()) {
            if (c.isoCode.equals(isoCode)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown ISO currency code: " + isoCode);
    }
}

