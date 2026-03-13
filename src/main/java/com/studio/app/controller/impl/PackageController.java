package com.studio.app.controller.impl;
import com.studio.app.controller.PackageApi;
import com.studio.app.dto.response.PackagePurchaseResponse;
import com.studio.app.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller implementation for single-resource package lookup.
 * Delegates to {@link PackageService} for retrieval.
 */
@RestController
@RequiredArgsConstructor
public class PackageController implements PackageApi {
    private final PackageService packageService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<PackagePurchaseResponse> getPackageById(Long packageId) {
        return ResponseEntity.ok(packageService.getPackageById(packageId));
    }
}
