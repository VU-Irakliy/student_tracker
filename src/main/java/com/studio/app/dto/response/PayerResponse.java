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

    private Long id;
    private Long studentId;
    private String studentName;
    private String fullName;
    private String phoneNumber;
    private String note;
}
