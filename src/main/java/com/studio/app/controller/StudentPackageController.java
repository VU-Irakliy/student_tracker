package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
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
 * REST controller for student-scoped package operations.
 * Base path: {@code /api/students/{studentId}/packages}
 */
@RestController
@RequestMapping(ApiConstants.STUDENTS)
@RequiredArgsConstructor
public class StudentPackageController {

    private final PackageService packageService;

    /**
     * Records a new package purchase for a student.
     * The amount paid may differ from any nominal list price.
     *
     * @param studentId the student ID
     * @param request   package details including actual amount paid
     * @return 201 Created with the new package purchase
     */
    @PostMapping("/{studentId}/packages")
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
    @GetMapping("/{studentId}/packages")
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
    @GetMapping("/{studentId}/packages/active")
    public ResponseEntity<List<PackagePurchaseResponse>> getActivePackages(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(packageService.getActivePackagesForStudent(studentId));
    }
}

