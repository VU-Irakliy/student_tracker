package com.studio.app.dto.response;

import lombok.*;

/**
 * Summary of records imported from a data snapshot.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataImportResultResponse {

    /** Number of student records imported. */
    private int students;

    /** Number of weekly schedule records imported. */
    private int weeklySchedules;

    /** Number of package purchase records imported. */
    private int packagePurchases;

    /** Number of class session records imported. */
    private int classSessions;

    /** Number of payer records imported. */
    private int payers;
}

