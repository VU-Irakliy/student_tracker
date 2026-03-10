package com.studio.app.controller;

import com.studio.app.dto.request.PackagePurchaseRequest;
import com.studio.app.dto.response.PackagePurchaseResponse;
import com.studio.app.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for package purchase management.
 * Base path: {@code /api/students/{studentId}/packages}
 * Single-resource: {@code /api/packages/{packageId}}
 */
@RestController
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    /**
     * Records a new package purchase for a student.
     * The amount paid may differ from any nominal list price.
     *
     * @param studentId the student ID
     * @param request   package details including actual amount paid
     * @return 201 Created with the new package purchase
     */
    @PostMapping("/api/students/{studentId}/packages")
    public ResponseEntity<PackagePurchaseResponse> purchasePackage(
            @PathVariable Long studentId,
            @Valid @RequestBody PackagePurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.purchasePackage(studentId, request));
    }

    /**
     * Returns all package purchases for a student (newest first).
     *
     * @param studentId the student ID
     * @return 200 OK with list of packages
     */
    @GetMapping("/api/students/{studentId}/packages")
    public ResponseEntity<List<PackagePurchaseResponse>> getPackagesForStudent(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(packageService.getPackagesForStudent(studentId));
    }

    /**
     * Returns only active (classes remaining > 0) packages for a student.
     *
     * @param studentId the student ID
     * @return 200 OK with active packages, oldest first (FIFO consumption order)
     */
    @GetMapping("/api/students/{studentId}/packages/active")
    public ResponseEntity<List<PackagePurchaseResponse>> getActivePackages(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(packageService.getActivePackagesForStudent(studentId));
    }

    /**
     * Returns a single package purchase by ID.
     *
     * @param packageId the package purchase ID
     * @return 200 OK or 404 Not Found
     */
    @GetMapping("/api/packages/{packageId}")
    public ResponseEntity<PackagePurchaseResponse> getPackageById(@PathVariable Long packageId) {
        return ResponseEntity.ok(packageService.getPackageById(packageId));
    }
}
