package com.studio.app.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response for a package purchase.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchaseResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private Integer totalClasses;
    private Integer classesRemaining;
    private BigDecimal amountPaid;
    private LocalDate paymentDate;
    private String description;
    private boolean exhausted;
    private LocalDateTime createdAt;
}
