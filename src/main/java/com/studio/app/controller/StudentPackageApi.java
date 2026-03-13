package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.PackagePurchaseRequest;
import com.studio.app.dto.response.PackagePurchaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API contract for student package operations.
 * Provides endpoints to purchase class packages and list all or active packages for a student.
 */
@Tag(name = "Student Packages", description = "Purchase and list class packages for a student")
@RequestMapping(ApiConstants.STUDENTS)
public interface StudentPackageApi {

    /**
     * Records a new package purchase for a student. The amount paid may differ from any nominal list price.
     *
     * @param studentId the ID of the student
     * @param request   the package purchase details
     * @return the created {@link PackagePurchaseResponse}
     */
    @Operation(summary = "Purchase a package",
            description = "Records a new package purchase for a student. The amount paid may differ from any nominal list price.")
    @PostMapping("/{studentId}/packages")
    ResponseEntity<PackagePurchaseResponse> purchasePackage(@PathVariable Long studentId,
                                                            @Valid @RequestBody PackagePurchaseRequest request);

    /**
     * Returns all package purchases for a student, newest first.
     *
     * @param studentId the ID of the student
     * @return a list of {@link PackagePurchaseResponse} objects
     */
    @Operation(summary = "List all packages", description = "Returns all package purchases for a student, newest first.")
    @GetMapping("/{studentId}/packages")
    ResponseEntity<List<PackagePurchaseResponse>> getPackagesForStudent(@PathVariable Long studentId);

    /**
     * Returns only packages with remaining classes, oldest first (FIFO consumption order).
     *
     * @param studentId the ID of the student
     * @return a list of active {@link PackagePurchaseResponse} objects
     */
    @Operation(summary = "List active packages",
            description = "Returns only packages with remaining classes, oldest first (FIFO consumption order).")
    @GetMapping("/{studentId}/packages/active")
    ResponseEntity<List<PackagePurchaseResponse>> getActivePackages(@PathVariable Long studentId);
}
