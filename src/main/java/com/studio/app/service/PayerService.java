package com.studio.app.service;

import com.studio.app.dto.request.PayerRequest;
import com.studio.app.dto.response.PayerResponse;

import java.util.List;

/**
 * Service interface for managing payers attached to a student.
 */
public interface PayerService {

    /**
     * Adds a new payer to a student.
     *
     * @param studentId the student ID
     * @param request   payer details
     * @return the created payer
     */
    PayerResponse addPayer(Long studentId, PayerRequest request);

    /**
     * Returns all active payers for a student.
     *
     * @param studentId the student ID
     * @return list of payers
     */
    List<PayerResponse> getPayersForStudent(Long studentId);

    /**
     * Updates an existing payer's details.
     *
     * @param studentId the student ID
     * @param payerId   the payer ID
     * @param request   updated details
     * @return the updated payer
     */
    PayerResponse updatePayer(Long studentId, Long payerId, PayerRequest request);

    /**
     * Soft-deletes a payer record.
     *
     * @param studentId the student ID
     * @param payerId   the payer ID
     */
    void removePayer(Long studentId, Long payerId);
}
