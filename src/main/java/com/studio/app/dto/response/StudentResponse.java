package com.studio.app.dto.response;

import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full student profile response, including their active weekly schedule and payers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private PricingType pricingType;
    private BigDecimal pricePerClass;
    private StudioTimezone timezone;
    private String notes;
    private LocalDateTime createdAt;
    private List<WeeklyScheduleResponse> weeklySchedules;

    /** People registered as payers for this student (e.g. parents, guardians). */
    private List<PayerResponse> payers;
}
