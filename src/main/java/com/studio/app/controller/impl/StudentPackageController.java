package com.studio.app.controller.impl;

import com.studio.app.controller.StudentPackageApi;
import com.studio.app.dto.request.PackagePurchaseRequest;
import com.studio.app.dto.response.PackagePurchaseResponse;
import com.studio.app.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller implementation for student package operations.
 * Delegates to {@link PackageService} for business logic.
 */
@RestController
@RequiredArgsConstructor
public class StudentPackageController implements StudentPackageApi {
    private final PackageService packageService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<PackagePurchaseResponse> purchasePackage(Long studentId, PackagePurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.purchasePackage(studentId, request));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<PackagePurchaseResponse>> getPackagesForStudent(Long studentId) {
        return ResponseEntity.ok(packageService.getPackagesForStudent(studentId));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<PackagePurchaseResponse>> getActivePackages(Long studentId) {
        return ResponseEntity.ok(packageService.getActivePackagesForStudent(studentId));
    }
}
