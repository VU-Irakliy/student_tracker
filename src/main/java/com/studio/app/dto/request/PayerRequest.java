package com.studio.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request body for adding or updating a payer linked to a student.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayerRequest {

    @NotBlank(message = "Payer full name is required")
    private String fullName;

    private String phoneNumber;

    /** Optional context note, e.g. "mother", "pays every 1st of month". */
    private String note;
}
