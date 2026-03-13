package com.studio.app.service;

import com.studio.app.dto.request.PackagePurchaseRequest;
import com.studio.app.dto.response.PackagePurchaseResponse;

import java.util.List;

/**
 * Service interface for managing package purchases.
 */
public interface PackageService {

    /**
     * Records a new package purchase for a student.
     *
     * @param studentId the student ID
     * @param request   the package details
     * @return the created package purchase
     */
    PackagePurchaseResponse purchasePackage(Long studentId, PackagePurchaseRequest request);

    /**
     * Returns all package purchases for a student (newest first).
     *
     * @param studentId the student ID
     * @return list of package purchase responses
     */
    List<PackagePurchaseResponse> getPackagesForStudent(Long studentId);

    /**
     * Returns only active (non-exhausted) packages for a student.
     *
     * @param studentId the student ID
     * @return active packages sorted by purchase date ascending (FIFO)
     */
    List<PackagePurchaseResponse> getActivePackagesForStudent(Long studentId);

    /**
     * Returns a single package purchase by ID.
     *
     * @param packageId the package ID
     * @return the package purchase response
     */
    PackagePurchaseResponse getPackageById(Long packageId);
}
