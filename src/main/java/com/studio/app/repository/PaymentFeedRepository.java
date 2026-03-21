package com.studio.app.repository;

import com.studio.app.dto.response.PaymentRecordResponse;
import com.studio.app.enums.Currency;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Native-query repository for the unified payments feed.
 */
@Repository
public class PaymentFeedRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<PaymentRecordResponse> findAllPayments(Pageable pageable) {
        String unionSql = """
                SELECT
                    p.payment_type,
                    p.payment_date_time,
                    p.amount,
                    p.currency,
                    p.student_id,
                    p.student_name,
                    p.session_id,
                    p.package_purchase_id,
                    p.note
                FROM (
                    SELECT
                        'SESSION' AS payment_type,
                        cs.payment_date_time AS payment_date_time,
                        cs.price_charged AS amount,
                        cs.currency AS currency,
                        s.id AS student_id,
                        CONCAT(s.first_name, ' ', s.last_name) AS student_name,
                        cs.id AS session_id,
                        CAST(NULL AS BIGINT) AS package_purchase_id,
                        cs.note AS note
                    FROM studio.class_sessions cs
                    JOIN studio.students s ON s.id = cs.student_id
                    WHERE cs.deleted = FALSE
                      AND s.deleted = FALSE
                      AND cs.payment_status = 'PAID'
                      AND cs.payment_date_time IS NOT NULL

                    UNION ALL

                    SELECT
                        'PACKAGE' AS payment_type,
                        CAST(pp.payment_date AS TIMESTAMP) AS payment_date_time,
                        pp.amount_paid AS amount,
                        pp.currency AS currency,
                        s.id AS student_id,
                        CONCAT(s.first_name, ' ', s.last_name) AS student_name,
                        CAST(NULL AS BIGINT) AS session_id,
                        pp.id AS package_purchase_id,
                        pp.description AS note
                    FROM studio.package_purchases pp
                    JOIN studio.students s ON s.id = pp.student_id
                    WHERE pp.deleted = FALSE
                      AND s.deleted = FALSE
                ) p
                ORDER BY p.payment_date_time DESC, p.payment_type ASC, p.student_id ASC
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM (
                    SELECT cs.id
                    FROM studio.class_sessions cs
                    JOIN studio.students s ON s.id = cs.student_id
                    WHERE cs.deleted = FALSE
                      AND s.deleted = FALSE
                      AND cs.payment_status = 'PAID'
                      AND cs.payment_date_time IS NOT NULL

                    UNION ALL

                    SELECT pp.id
                    FROM studio.package_purchases pp
                    JOIN studio.students s ON s.id = pp.student_id
                    WHERE pp.deleted = FALSE
                      AND s.deleted = FALSE
                ) p
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(unionSql)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Number total = (Number) entityManager.createNativeQuery(countSql).getSingleResult();

        List<PaymentRecordResponse> content = rows.stream().map(this::toPaymentRecord).toList();
        return new PageImpl<>(content, pageable, total.longValue());
    }

    private PaymentRecordResponse toPaymentRecord(Object[] row) {
        return PaymentRecordResponse.builder()
                .paymentType((String) row[0])
                .paymentDateTime(asLocalDateTime(row[1]))
                .amount((BigDecimal) row[2])
                .currency(row[3] == null ? null : Currency.valueOf((String) row[3]))
                .studentId(row[4] == null ? null : ((Number) row[4]).longValue())
                .studentName((String) row[5])
                .sessionId(row[6] == null ? null : ((Number) row[6]).longValue())
                .packagePurchaseId(row[7] == null ? null : ((Number) row[7]).longValue())
                .note((String) row[8])
                .build();
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString());
    }
}

