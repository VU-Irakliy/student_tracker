package com.studio.app.service.impl;

import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.service.DebtorStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;

/**
 * Nightly debtor-status recomputation based on each student's local timezone.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DebtorStatusServiceImpl implements DebtorStatusService {

    private static final LocalTime DEBTOR_CHECK_TIME = LocalTime.of(22, 0);

    private final StudentRepository studentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final Clock clock;

    /** {@inheritDoc} */
    @Override
    public void refreshDebtorStatuses() {
        refreshDebtorStatuses(false);
    }

    /** {@inheritDoc} */
    @Override
    public void refreshDebtorStatuses(boolean ignoreCutoff) {
        var now = Instant.now(clock);
        var changedStudents = new ArrayList<com.studio.app.entity.Student>();

        for (var student : studentRepository.findAllByDeletedFalse()) {
            var localNow = ZonedDateTime.ofInstant(now, student.getTimezone().toZoneId());
            if (!ignoreCutoff && localNow.toLocalTime().isBefore(DEBTOR_CHECK_TIME)) {
                continue;
            }

            var hasDebt = classSessionRepository.existsUnpaidOccurredSessionForStudent(
                    student.getId(), localNow.toLocalDate(), localNow.toLocalTime());

            if (student.isDebtor() != hasDebt) {
                student.setDebtor(hasDebt);
                changedStudents.add(student);
            }
        }

        if (!changedStudents.isEmpty()) {
            studentRepository.saveAll(changedStudents);
        }
    }
}



