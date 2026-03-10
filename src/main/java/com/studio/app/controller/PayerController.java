package com.studio.app.controller;

import com.studio.app.dto.request.PayerRequest;
import com.studio.app.dto.response.PayerResponse;
import com.studio.app.service.PayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing payers attached to a student.
 *
 * <p>Use this when a bank transfer arrives and you want to record
 * who sent the money and for which student.
 *
 * <p>Base path: {@code /api/students}
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class PayerController {

    private final PayerService payerService;

    /**
     * Adds a payer to a student.
     * Call this when you receive a transfer and want to record the sender.
     *
     * @param studentId the student ID
     * @param request   payer name, phone, and optional note
     * @return 201 Created with the new payer record
     */
    @PostMapping("/{studentId}/payers")
    public ResponseEntity<PayerResponse> addPayer(
            @PathVariable Long studentId,
            @Valid @RequestBody PayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payerService.addPayer(studentId, request));
    }

    /**
     * Returns all payers registered for a student.
     *
     * @param studentId the student ID
     * @return 200 OK with list of payers
     */
    @GetMapping("/{studentId}/payers")
    public ResponseEntity<List<PayerResponse>> getPayersForStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(payerService.getPayersForStudent(studentId));
    }

    /**
     * Updates a payer's details (e.g. phone number changed).
     *
     * @param studentId the student ID
     * @param payerId   the payer ID
     * @param request   updated fields
     * @return 200 OK with updated payer
     */
    @PostMapping("/{studentId}/payers/{payerId}")
    public ResponseEntity<PayerResponse> updatePayer(
            @PathVariable Long studentId,
            @PathVariable Long payerId,
            @Valid @RequestBody PayerRequest request) {
        return ResponseEntity.ok(payerService.updatePayer(studentId, payerId, request));
    }

    /**
     * Soft-deletes a payer record.
     *
     * @param studentId the student ID
     * @param payerId   the payer ID
     * @return 204 No Content
     */
    @PostMapping("/{studentId}/payers/{payerId}/delete")
    public ResponseEntity<Void> removePayer(
            @PathVariable Long studentId,
            @PathVariable Long payerId) {
        payerService.removePayer(studentId, payerId);
        return ResponseEntity.noContent().build();
    }
}
