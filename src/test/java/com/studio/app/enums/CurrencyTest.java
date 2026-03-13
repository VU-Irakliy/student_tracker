package com.studio.app.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CurrencyTest {

    @Test
    void shouldHaveCorrectIsoCodes() {
        assertThat(Currency.DOLLARS.getIsoCode()).isEqualTo("USD");
        assertThat(Currency.EUROS.getIsoCode()).isEqualTo("EUR");
        assertThat(Currency.RUBLES.getIsoCode()).isEqualTo("RUB");
    }

    @Test
    void fromIsoCode_shouldReturnCorrectEnum() {
        assertThat(Currency.fromIsoCode("USD")).isEqualTo(Currency.DOLLARS);
        assertThat(Currency.fromIsoCode("EUR")).isEqualTo(Currency.EUROS);
        assertThat(Currency.fromIsoCode("RUB")).isEqualTo(Currency.RUBLES);
    }

    @Test
    void fromIsoCode_shouldThrowForUnknownCode() {
        assertThatThrownBy(() -> Currency.fromIsoCode("GBP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown ISO currency code");
    }

    @Test
    void shouldHaveExactlyThreeValues() {
        assertThat(Currency.values()).hasSize(3);
    }
}

