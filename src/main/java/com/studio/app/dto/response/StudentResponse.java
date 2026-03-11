package com.studio.app.dto.response;

import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private String phoneNumber;
    private PricingType pricingType;
    private BigDecimal pricePerClass;
    private Currency currency;

    /**
     * The same {@code pricePerClass} converted into all supported currencies.
     * Key = target {@link Currency}, value = converted amount.
     */
    private Map<Currency, BigDecimal> convertedPrices;

    private StudioTimezone timezone;
    private String notes;
    private LocalDateTime createdAt;
    private List<WeeklyScheduleResponse> weeklySchedules;

    /** People registered as payers for this student (e.g. parents, guardians). */
    private List<PayerResponse> payers;
}
