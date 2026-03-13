package com.studio.app.service.impl;

import com.studio.app.dto.request.PackagePurchaseRequest;
import com.studio.app.dto.response.PackagePurchaseResponse;
import com.studio.app.entity.PackagePurchase;
import com.studio.app.entity.Student;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.PackagePurchaseMapper;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.service.CurrencyConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceImplTest {

    @Mock PackagePurchaseRepository packageRepository;
    @Mock StudentRepository studentRepository;
    @Mock PackagePurchaseMapper packageMapper;
    @Mock CurrencyConversionService currencyConversionService;

    @InjectMocks PackageServiceImpl packageService;

    private Student student;
    private PackagePurchase pkg;
    private PackagePurchaseResponse pkgResponse;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L).firstName("Ivan").lastName("Petrov")
                .pricingType(PricingType.PACKAGE)
                .currency(Currency.RUBLES)
                .timezone(StudioTimezone.RUSSIA_MOSCOW)
                .build();

        pkg = PackagePurchase.builder()
                .id(10L).student(student)
                .totalClasses(10).classesRemaining(10)
                .amountPaid(new BigDecimal("15000.00"))
                .currency(Currency.RUBLES)
                .paymentDate(LocalDate.of(2026, 3, 1))
                .description("March bundle")
                .build();

        pkgResponse = PackagePurchaseResponse.builder()
                .id(10L).studentId(1L).studentName("Ivan Petrov")
                .totalClasses(10).classesRemaining(10)
                .amountPaid(new BigDecimal("15000.00"))
                .currency(Currency.RUBLES)
                .paymentDate(LocalDate.of(2026, 3, 1))
                .description("March bundle")
                .exhausted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    class PurchasePackage {

        @Test
        void shouldCreatePackageWithStudentCurrency() {
            var request = PackagePurchaseRequest.builder()
                    .totalClasses(10)
                    .amountPaid(new BigDecimal("15000.00"))
                    .paymentDate(LocalDate.of(2026, 3, 1))
                    .description("March bundle")
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(student));
            when(packageRepository.save(any())).thenReturn(pkg);
            when(packageMapper.toResponse(pkg)).thenReturn(pkgResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            packageService.purchasePackage(1L, request);

            ArgumentCaptor<PackagePurchase> captor = ArgumentCaptor.forClass(PackagePurchase.class);
            verify(packageRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getCurrency()).isEqualTo(Currency.RUBLES);
            assertThat(saved.getTotalClasses()).isEqualTo(10);
            assertThat(saved.getClassesRemaining()).isEqualTo(10);
        }

        @Test
        void shouldUseRequestCurrencyWhenProvided() {
            var request = PackagePurchaseRequest.builder()
                    .totalClasses(5)
                    .amountPaid(new BigDecimal("100.00"))
                    .currency(Currency.DOLLARS)
                    .paymentDate(LocalDate.of(2026, 3, 1))
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(student));
            when(packageRepository.save(any())).thenReturn(pkg);
            when(packageMapper.toResponse(pkg)).thenReturn(pkgResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            packageService.purchasePackage(1L, request);

            ArgumentCaptor<PackagePurchase> captor = ArgumentCaptor.forClass(PackagePurchase.class);
            verify(packageRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.DOLLARS);
        }

        @Test
        void shouldThrowNotFound_whenStudentMissing() {
            when(studentRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> packageService.purchasePackage(99L,
                    PackagePurchaseRequest.builder()
                            .totalClasses(5).amountPaid(new BigDecimal("100.00"))
                            .paymentDate(LocalDate.now()).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldEnrichResponseWithConvertedAmounts() {
            var request = PackagePurchaseRequest.builder()
                    .totalClasses(10)
                    .amountPaid(new BigDecimal("15000.00"))
                    .paymentDate(LocalDate.of(2026, 3, 1))
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(student));
            when(packageRepository.save(any())).thenReturn(pkg);
            when(packageMapper.toResponse(pkg)).thenReturn(pkgResponse);
            when(currencyConversionService.convertToAll(new BigDecimal("15000.00"), Currency.RUBLES))
                    .thenReturn(Map.of(
                            Currency.RUBLES, new BigDecimal("15000.00"),
                            Currency.DOLLARS, new BigDecimal("162.00"),
                            Currency.EUROS, new BigDecimal("148.50")
                    ));

            var result = packageService.purchasePackage(1L, request);

            assertThat(result.getConvertedAmountPaid()).containsKey(Currency.DOLLARS);
            assertThat(result.getConvertedAmountPaid()).containsKey(Currency.EUROS);
        }
    }

    @Nested
    class GetPackages {

        @Test
        void getPackagesForStudent_shouldReturnAllPackages() {
            when(packageRepository.findByStudentIdAndDeletedFalseOrderByPaymentDateDesc(1L))
                    .thenReturn(List.of(pkg));
            when(packageMapper.toResponseList(List.of(pkg)))
                    .thenReturn(List.of(pkgResponse));
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = packageService.getPackagesForStudent(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        void getActivePackages_shouldReturnOnlyNonExhausted() {
            when(packageRepository.findActivePackagesByStudent(1L))
                    .thenReturn(List.of(pkg));
            when(packageMapper.toResponseList(List.of(pkg)))
                    .thenReturn(List.of(pkgResponse));
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = packageService.getActivePackagesForStudent(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        void getPackageById_shouldReturnPackage() {
            when(packageRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(pkg));
            when(packageMapper.toResponse(pkg)).thenReturn(pkgResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = packageService.getPackageById(10L);

            assertThat(result.getId()).isEqualTo(10L);
        }

        @Test
        void getPackageById_shouldThrowNotFound() {
            when(packageRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> packageService.getPackageById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

