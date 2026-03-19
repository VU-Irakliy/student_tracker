package com.studio.app.service.impl;

import com.studio.app.dto.request.PackagePurchaseRequest;
import com.studio.app.enums.Currency;
import com.studio.app.exception.BadRequestException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.service.PackageService;
import com.studio.app.support.StubCurrencyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(StubCurrencyTestConfig.class)
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/testdata/service/package/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PackageServiceImplTest {

    @Autowired
    private PackageService packageService;

    @Autowired
    private PackagePurchaseRepository packageRepository;

    @Test
    void shouldCreatePackageWhenCurrencyProvided() {
        var result = packageService.purchasePackage(1L, PackagePurchaseRequest.builder()
                .totalClasses(10)
                .amountPaid(new BigDecimal("15000.00"))
                .currency(Currency.RUBLES)
                .paymentDate(LocalDate.of(2026, 3, 20))
                .description("March bundle")
                .build());

        var persisted = packageRepository.findByIdAndDeletedFalse(result.getId()).orElseThrow();
        assertThat(persisted.getCurrency()).isEqualTo(Currency.RUBLES);
        assertThat(persisted.getClassesRemaining()).isEqualTo(10);
    }

    @Test
    void shouldUseRequestCurrencyWhenProvided() {
        var result = packageService.purchasePackage(1L, PackagePurchaseRequest.builder()
                .totalClasses(5)
                .amountPaid(new BigDecimal("100.00"))
                .currency(Currency.DOLLARS)
                .paymentDate(LocalDate.of(2026, 3, 20))
                .build());

        var persisted = packageRepository.findByIdAndDeletedFalse(result.getId()).orElseThrow();
        assertThat(persisted.getCurrency()).isEqualTo(Currency.DOLLARS);
    }

    @Test
    void shouldThrowNotFoundWhenStudentMissing() {
        assertThatThrownBy(() -> packageService.purchasePackage(99L, PackagePurchaseRequest.builder()
                .totalClasses(5)
                .amountPaid(new BigDecimal("100.00"))
                .currency(Currency.RUBLES)
                .paymentDate(LocalDate.of(2026, 3, 20))
                .build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldEnrichResponseWithConvertedAmounts() {
        var result = packageService.purchasePackage(1L, PackagePurchaseRequest.builder()
                .totalClasses(10)
                .amountPaid(new BigDecimal("15000.00"))
                .currency(Currency.RUBLES)
                .paymentDate(LocalDate.of(2026, 3, 20))
                .build());

        assertThat(result.getConvertedAmountPaid()).containsKeys(Currency.EUROS, Currency.DOLLARS, Currency.RUBLES);
    }

    @Test
    void getPackagesForStudentShouldReturnAllPackages() {
        var result = packageService.getPackagesForStudent(1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void getActivePackagesShouldReturnOnlyNonExhausted() {
        var result = packageService.getActivePackagesForStudent(1L);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().isExhausted()).isFalse();
    }

    @Test
    void getPackageByIdShouldReturnPackage() {
        var result = packageService.getPackageById(10L);
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getPackageByIdShouldThrowNotFound() {
        assertThatThrownBy(() -> packageService.getPackageById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectWhenCurrencyMissing() {
        assertThatThrownBy(() -> packageService.purchasePackage(1L, PackagePurchaseRequest.builder()
                .totalClasses(5)
                .amountPaid(new BigDecimal("100.00"))
                .paymentDate(LocalDate.of(2026, 3, 20))
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void shouldRejectWhenRequestIsNull() {
        assertThatThrownBy(() -> packageService.purchasePackage(1L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("request");
    }
}

