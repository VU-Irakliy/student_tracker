package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.response.PackagePurchaseResponse;
import com.studio.app.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for single-resource package operations.
 * Base path: {@code /api/packages}
 */
@RestController
@RequestMapping(ApiConstants.PACKAGES)
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    /**
     * Returns a single package purchase by ID.
     *
     * @param packageId the package purchase ID
     * @return 200 OK or 404 Not Found
     */
    @GetMapping("/{packageId}")
    public ResponseEntity<PackagePurchaseResponse> getPackageById(@PathVariable Long packageId) {
        return ResponseEntity.ok(packageService.getPackageById(packageId));
    }
}
