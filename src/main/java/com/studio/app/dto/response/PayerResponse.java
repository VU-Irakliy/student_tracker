package com.studio.app.dto.response;

import lombok.*;

/**
 * Response for a payer record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayerResponse {

    /** Identifier of the payer record. */
    private Long id;

    /** Identifier of the student this payer is linked to. */
    private Long studentId;

    /** Convenience student name for UI display. */
    private String studentName;

    /** Full name of the payer. */
    private String fullName;

    /** Contact phone number of the payer. */
    private String phoneNumber;

    /** Optional note about the payer relationship or payment habits. */
    private String note;
}
