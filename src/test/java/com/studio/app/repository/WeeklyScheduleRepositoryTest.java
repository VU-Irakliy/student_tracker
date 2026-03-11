package com.studio.app.repository;

import com.studio.app.entity.Student;
import com.studio.app.entity.WeeklySchedule;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class WeeklyScheduleRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private WeeklyScheduleRepository scheduleRepository;

    private Student student;
    private WeeklySchedule mondaySchedule;

    @BeforeEach
    void setUp() {
        student = em.persistAndFlush(Student.builder()
                .firstName("Ana").lastName("Garcia")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("35.00"))
                .timezone(StudioTimezone.SPAIN)
                .build());

        mondaySchedule = em.persistAndFlush(WeeklySchedule.builder()
                .student(student)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .durationMinutes(60)
                .build());

        // deleted schedule
        var deleted = WeeklySchedule.builder()
                .student(student)
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(14, 0))
                .durationMinutes(45)
                .build();
        deleted.setDeleted(true);
        em.persistAndFlush(deleted);
    }

    @Test
    void findByStudentIdAndDeletedFalse_excludesDeleted() {
        var result = scheduleRepository.findByStudentIdAndDeletedFalse(student.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void findByIdAndStudentIdAndDeletedFalse_found() {
        assertThat(scheduleRepository.findByIdAndStudentIdAndDeletedFalse(
                mondaySchedule.getId(), student.getId()))
                .isPresent();
    }

    @Test
    void findByIdAndStudentIdAndDeletedFalse_wrongStudent() {
        assertThat(scheduleRepository.findByIdAndStudentIdAndDeletedFalse(
                mondaySchedule.getId(), 999L))
                .isEmpty();
    }

    @Test
    void existsByStudentIdAndDayOfWeekAndDeletedFalse_true() {
        assertThat(scheduleRepository.existsByStudentIdAndDayOfWeekAndDeletedFalse(
                student.getId(), DayOfWeek.MONDAY))
                .isTrue();
    }

    @Test
    void existsByStudentIdAndDayOfWeekAndDeletedFalse_falseForDeletedSchedule() {
        assertThat(scheduleRepository.existsByStudentIdAndDayOfWeekAndDeletedFalse(
                student.getId(), DayOfWeek.FRIDAY))
                .isFalse();
    }

    @Test
    void existsByStudentIdAndDayOfWeekAndDeletedFalse_falseForUnusedDay() {
        assertThat(scheduleRepository.existsByStudentIdAndDayOfWeekAndDeletedFalse(
                student.getId(), DayOfWeek.WEDNESDAY))
                .isFalse();
    }
}

