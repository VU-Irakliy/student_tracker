package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.response.PackagePurchaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST API contract for single-resource package operations.
 * Provides an endpoint to look up a package purchase by its ID.
 */
@Tag(name = "Packages", description = "Single-resource package lookup")
@RequestMapping(ApiConstants.PACKAGES)
public interface PackageApi {

    /**
     * Retrieves a single package purchase by its ID.
     *
     * @param packageId the ID of the package to retrieve
     * @return the {@link PackagePurchaseResponse} for the requested package
     */
    @Operation(summary = "Get a package by ID", description = "Returns a single package purchase by its ID.")
    @GetMapping("/{packageId}")
    ResponseEntity<PackagePurchaseResponse> getPackageById(@PathVariable Long packageId);
}
