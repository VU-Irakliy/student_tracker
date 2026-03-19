package com.studio.app.service.impl;

import com.studio.app.repository.StudentRepository;
import com.studio.app.service.DebtorStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/testdata/service/debtor/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DebtorStatusServiceImplTest {

    @Autowired
    private DebtorStatusService debtorStatusService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MutableClock mutableClock;

    @BeforeEach
    void resetClock() {
        mutableClock.setInstant(Instant.parse("2026-03-15T22:30:00Z"));
    }

    @Test
    void shouldProcessOnlyStudentsWhoseLocalTimeIsAfter22() {
        // 20:30 UTC => 21:30 in Spain, 23:30 in Moscow
        mutableClock.setInstant(Instant.parse("2026-03-15T20:30:00Z"));

        debtorStatusService.refreshDebtorStatuses();

        var spainStudent = studentRepository.findByIdAndDeletedFalse(1L).orElseThrow();
        var moscowStudent = studentRepository.findByIdAndDeletedFalse(2L).orElseThrow();

        assertThat(spainStudent.isDebtor()).isFalse();
        assertThat(moscowStudent.isDebtor()).isTrue();
    }

    @Test
    void shouldClearDebtorWhenAllUnpaidSessionsAreResolved() {
        var debtorStudent = studentRepository.findByIdAndDeletedFalse(3L).orElseThrow();
        assertThat(debtorStudent.isDebtor()).isTrue();

        debtorStatusService.refreshDebtorStatuses();

        debtorStudent = studentRepository.findByIdAndDeletedFalse(3L).orElseThrow();
        assertThat(debtorStudent.isDebtor()).isFalse();
    }

    @Test
    void shouldNotChangeFlagsWhenAlreadyCorrect() {
        var studentBefore = studentRepository.findByIdAndDeletedFalse(4L).orElseThrow();
        assertThat(studentBefore.isDebtor()).isFalse();

        debtorStatusService.refreshDebtorStatuses();

        var studentAfter = studentRepository.findByIdAndDeletedFalse(4L).orElseThrow();
        assertThat(studentAfter.isDebtor()).isFalse();
    }

    @Test
    void shouldBypassCutoffWhenRequestedForStartupCatchUp() {
        // 20:30 UTC => 21:30 in Spain (before nightly cutoff)
        mutableClock.setInstant(Instant.parse("2026-03-15T20:30:00Z"));

        debtorStatusService.refreshDebtorStatuses(true);

        var spainStudent = studentRepository.findByIdAndDeletedFalse(1L).orElseThrow();
        assertThat(spainStudent.isDebtor()).isTrue();
    }

    @TestConfiguration
    static class DebtorClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2026-03-15T22:30:00Z"), ZoneOffset.UTC);
        }
    }

    static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
