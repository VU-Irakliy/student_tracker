package com.studio.app.service.impl;

import com.studio.app.dto.request.PackagePurchaseRequest;
import com.studio.app.dto.response.PackagePurchaseResponse;
import com.studio.app.entity.PackagePurchase;
import com.studio.app.exception.BadRequestException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.PackagePurchaseMapper;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.service.CurrencyConversionService;
import com.studio.app.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link PackageService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PackageServiceImpl implements PackageService {

    private final PackagePurchaseRepository packageRepository;
    private final StudentRepository studentRepository;
    private final PackagePurchaseMapper packageMapper;
    private final CurrencyConversionService currencyConversionService;

    /** {@inheritDoc} */
    @Override
    public PackagePurchaseResponse purchasePackage(Long studentId, PackagePurchaseRequest request) {
        validatePurchaseRequest(request);

        var student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        var pkg = PackagePurchase.builder()
                .student(student)
                .totalClasses(request.getTotalClasses())
                .classesRemaining(request.getTotalClasses())
                .amountPaid(request.getAmountPaid())
                .currency(request.getCurrency())
                .paymentDate(request.getPaymentDate())
                .description(request.getDescription())
                .build();

        return enrichWithConvertedAmountPaid(packageMapper.toResponse(packageRepository.save(pkg)));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<PackagePurchaseResponse> getPackagesForStudent(Long studentId) {
        studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        return packageMapper.toResponseList(
                packageRepository.findByStudentIdAndDeletedFalseOrderByPaymentDateDesc(studentId))
                .stream().map(this::enrichWithConvertedAmountPaid).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<PackagePurchaseResponse> getActivePackagesForStudent(Long studentId) {
        studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        return packageMapper.toResponseList(
                packageRepository.findActivePackagesByStudent(studentId))
                .stream().map(this::enrichWithConvertedAmountPaid).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PackagePurchaseResponse getPackageById(Long packageId) {
        return enrichWithConvertedAmountPaid(packageMapper.toResponse(
                packageRepository.findByIdAndDeletedFalse(packageId)
                        .orElseThrow(() -> new ResourceNotFoundException("PackagePurchase", packageId))));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void validatePurchaseRequest(PackagePurchaseRequest request) {
        if (request == null) {
            throw new BadRequestException("Package purchase request is required");
        }
        if (request.getTotalClasses() == null) {
            throw new BadRequestException("totalClasses is required");
        }
        if (request.getAmountPaid() == null) {
            throw new BadRequestException("amountPaid is required");
        }
        if (request.getCurrency() == null) {
            throw new BadRequestException("currency is required");
        }
        if (request.getPaymentDate() == null) {
            throw new BadRequestException("paymentDate is required");
        }
    }

    /**
     * Populates the {@code convertedAmountPaid} field on a response by delegating
     * to the {@link CurrencyConversionService}.
     */
    private PackagePurchaseResponse enrichWithConvertedAmountPaid(PackagePurchaseResponse response) {
        if (response.getAmountPaid() != null && response.getCurrency() != null) {
            response.setConvertedAmountPaid(
                    currencyConversionService.convertToAll(
                            response.getAmountPaid(), response.getCurrency()));
        }
        return response;
    }
}
