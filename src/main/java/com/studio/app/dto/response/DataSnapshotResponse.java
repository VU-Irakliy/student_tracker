package com.studio.app.dto.response;

import com.studio.app.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Full database snapshot used to export/import application data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSnapshotResponse {

    /** UTC timestamp when this snapshot was generated. */
    private LocalDateTime exportedAtUtc;

    /** Schema version marker for future compatibility checks. */
    @Builder.Default
    private String snapshotVersion = "1";

    /** Student records. */
    @Builder.Default
    private List<StudentRow> students = new ArrayList<>();

    /** Weekly schedule records. */
    @Builder.Default
    private List<WeeklyScheduleRow> weeklySchedules = new ArrayList<>();

    /** Package purchase records. */
    @Builder.Default
    private List<PackagePurchaseRow> packagePurchases = new ArrayList<>();

    /** Class session records. */
    @Builder.Default
    private List<ClassSessionRow> classSessions = new ArrayList<>();

    /** Payer records. */
    @Builder.Default
    private List<PayerRow> payers = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentRow {
        private Long id;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private PricingType pricingType;
        private BigDecimal pricePerClass;
        private Currency currency;
        private StudioTimezone timezone;
        private StudentClassType classType;
        private String notes;
        private boolean debtor;
        private boolean deleted;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyScheduleRow {
        private Long id;
        private Long studentId;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private Integer durationMinutes;
        private Long effectiveFromEpochDay;
        private boolean deleted;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PackagePurchaseRow {
        private Long id;
        private Long studentId;
        private Integer totalClasses;
        private Integer classesRemaining;
        private BigDecimal amountPaid;
        private Currency currency;
        private LocalDate paymentDate;
        private String description;
        private boolean deleted;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassSessionRow {
        private Long id;
        private Long studentId;
        private Long weeklyScheduleId;
        private LocalDate classDate;
        private LocalTime startTime;
        private Integer durationMinutes;
        private ClassStatus status;
        private PaymentStatus paymentStatus;
        private BigDecimal priceCharged;
        private Currency currency;
        private Long packagePurchaseId;
        private boolean oneOff;
        private String note;
        private boolean deleted;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayerRow {
        private Long id;
        private Long studentId;
        private String fullName;
        private String phoneNumber;
        private String note;
        private boolean deleted;
    }
}

