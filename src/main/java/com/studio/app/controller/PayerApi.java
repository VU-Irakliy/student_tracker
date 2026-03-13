package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.PayerRequest;
import com.studio.app.dto.response.PayerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API contract for student payer operations.
 * Provides endpoints to create, list, update, and soft-delete payers attached to a student.
 */
@Tag(name = "Student Payers", description = "Manage payers (bank transfer senders) attached to a student")
@RequestMapping(ApiConstants.STUDENTS)
public interface PayerApi {

    /**
     * Records a new payer for the given student.
     *
     * @param studentId the ID of the student
     * @param request   the payer details
     * @return the created {@link PayerResponse}
     */
    @Operation(summary = "Add a payer", description = "Records a new payer for a student (e.g. when a bank transfer arrives).")
    @PostMapping("/{studentId}/payers")
    ResponseEntity<PayerResponse> addPayer(@PathVariable Long studentId,
                                           @Valid @RequestBody PayerRequest request);

    /**
     * Returns all payers registered for a student.
     *
     * @param studentId the ID of the student
     * @return a list of {@link PayerResponse} objects
     */
    @Operation(summary = "List payers", description = "Returns all payers registered for a student.")
    @GetMapping("/{studentId}/payers")
    ResponseEntity<List<PayerResponse>> getPayersForStudent(@PathVariable Long studentId);

    /**
     * Updates an existing payer's details (name, phone, note).
     *
     * @param studentId the ID of the student
     * @param payerId   the ID of the payer to update
     * @param request   the updated payer details
     * @return the updated {@link PayerResponse}
     */
    @Operation(summary = "Update a payer", description = "Updates a payer's details (name, phone, note).")
    @PostMapping("/{studentId}/payers/{payerId}")
    ResponseEntity<PayerResponse> updatePayer(@PathVariable Long studentId,
                                              @PathVariable Long payerId,
                                              @Valid @RequestBody PayerRequest request);

    /**
     * Soft-deletes a payer record.
     *
     * @param studentId the ID of the student
     * @param payerId   the ID of the payer to remove
     * @return 204 No Content on success
     */
    @Operation(summary = "Delete a payer", description = "Soft-deletes a payer record.")
    @PostMapping("/{studentId}/payers/{payerId}/delete")
    ResponseEntity<Void> removePayer(@PathVariable Long studentId, @PathVariable Long payerId);
}
