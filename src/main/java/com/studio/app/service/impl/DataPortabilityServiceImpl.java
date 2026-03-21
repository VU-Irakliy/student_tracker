package com.studio.app.service.impl;

import com.studio.app.dto.response.DataImportResultResponse;
import com.studio.app.dto.response.DataSnapshotResponse;
import com.studio.app.entity.*;
import com.studio.app.enums.StudentClassType;
import com.studio.app.exception.BadRequestException;
import com.studio.app.repository.*;
import com.studio.app.service.DataPortabilityService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Default implementation for import/export snapshot operations.
 */
@Lazy
@Service
@RequiredArgsConstructor
public class DataPortabilityServiceImpl implements DataPortabilityService {

    private final StudentRepository studentRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final PackagePurchaseRepository packagePurchaseRepository;
    private final ClassSessionRepository classSessionRepository;
    private final PayerRepository payerRepository;
    private final EntityManager entityManager;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public DataSnapshotResponse exportData() {
        var students = studentRepository.findAll();
        var schedules = weeklyScheduleRepository.findAll();
        var packages = packagePurchaseRepository.findAll();
        var sessions = classSessionRepository.findAll();
        var payers = payerRepository.findAll();

        students.sort(Comparator.comparing(Student::getId));
        schedules.sort(Comparator.comparing(WeeklySchedule::getId));
        packages.sort(Comparator.comparing(PackagePurchase::getId));
        sessions.sort(Comparator.comparing(ClassSession::getId));
        payers.sort(Comparator.comparing(Payer::getId));

        return DataSnapshotResponse.builder()
                .exportedAtUtc(LocalDateTime.now())
                .snapshotVersion("2")
                .students(students.stream().map(this::toStudentRow).toList())
                .weeklySchedules(schedules.stream().map(this::toScheduleRow).toList())
                .packagePurchases(packages.stream().map(this::toPackageRow).toList())
                .classSessions(sessions.stream().map(this::toSessionRow).toList())
                .payers(payers.stream().map(this::toPayerRow).toList())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DataImportResultResponse importData(DataSnapshotResponse snapshot) {
        if (snapshot == null) {
            throw new BadRequestException("Snapshot payload is required");
        }

        var students = Optional.ofNullable(snapshot.getStudents()).orElseGet(List::of);
        var schedules = Optional.ofNullable(snapshot.getWeeklySchedules()).orElseGet(List::of);
        var packages = Optional.ofNullable(snapshot.getPackagePurchases()).orElseGet(List::of);
        var sessions = Optional.ofNullable(snapshot.getClassSessions()).orElseGet(List::of);
        var payers = Optional.ofNullable(snapshot.getPayers()).orElseGet(List::of);

        clearAllTables();

        // Old snapshot ids -> newly inserted entities
        Map<Long, Student> studentMap = new HashMap<>();
        for (var row : students) {
            var saved = studentRepository.save(Student.builder()
                    .firstName(row.getFirstName())
                    .lastName(row.getLastName())
                    .phoneNumber(row.getPhoneNumber())
                    .pricingType(row.getPricingType())
                    .pricePerClass(row.getPricePerClass())
                    .currency(row.getCurrency())
                    .timezone(row.getTimezone())
                    .classType(Optional.ofNullable(row.getClassType()).orElse(StudentClassType.CASUAL))
                    .startDate(row.getStartDate())
                    .holidayMode(row.isHolidayMode())
                    .holidayFrom(row.getHolidayFrom())
                    .holidayTo(row.getHolidayTo())
                    .stoppedAttending(row.isStoppedAttending())
                    .notes(row.getNotes())
                    .debtor(row.isDebtor())
                    .build());
            saved.setDeleted(row.isDeleted());
            studentMap.put(requireId(row.getId(), "student"), studentRepository.save(saved));
        }

        Map<Long, WeeklySchedule> scheduleMap = new HashMap<>();
        for (var row : schedules) {
            var student = requireMapped(studentMap, row.getStudentId(), "student", "weekly schedule");
            var saved = weeklyScheduleRepository.save(WeeklySchedule.builder()
                    .student(student)
                    .dayOfWeek(row.getDayOfWeek())
                    .startTime(row.getStartTime())
                    .durationMinutes(row.getDurationMinutes())
                    .effectiveFromEpochDay(row.getEffectiveFromEpochDay())
                    .build());
            saved.setDeleted(row.isDeleted());
            scheduleMap.put(requireId(row.getId(), "weekly schedule"), weeklyScheduleRepository.save(saved));
        }

        Map<Long, PackagePurchase> packageMap = new HashMap<>();
        for (var row : packages) {
            var student = requireMapped(studentMap, row.getStudentId(), "student", "package purchase");
            var saved = packagePurchaseRepository.save(PackagePurchase.builder()
                    .student(student)
                    .totalClasses(row.getTotalClasses())
                    .classesRemaining(row.getClassesRemaining())
                    .amountPaid(row.getAmountPaid())
                    .currency(row.getCurrency())
                    .paymentDate(row.getPaymentDate())
                    .description(row.getDescription())
                    .build());
            saved.setDeleted(row.isDeleted());
            packageMap.put(requireId(row.getId(), "package purchase"), packagePurchaseRepository.save(saved));
        }

        for (var row : sessions) {
            var student = requireMapped(studentMap, row.getStudentId(), "student", "class session");
            var weeklySchedule = row.getWeeklyScheduleId() == null
                    ? null
                    : requireMapped(scheduleMap, row.getWeeklyScheduleId(), "weekly schedule", "class session");
            var packagePurchase = row.getPackagePurchaseId() == null
                    ? null
                    : requireMapped(packageMap, row.getPackagePurchaseId(), "package purchase", "class session");

            var saved = classSessionRepository.save(ClassSession.builder()
                    .student(student)
                    .weeklySchedule(weeklySchedule)
                    .classDate(row.getClassDate())
                    .startTime(row.getStartTime())
                    .timezone(Optional.ofNullable(row.getTimezone()).orElse(student.getTimezone()))
                    .durationMinutes(row.getDurationMinutes())
                    .status(row.getStatus())
                    .paymentStatus(row.getPaymentStatus())
                    .priceCharged(row.getPriceCharged())
                    .currency(row.getCurrency())
                    .packagePurchase(packagePurchase)
                    .oneOff(row.isOneOff())
                    .note(row.getNote())
                    .build());
            saved.setDeleted(row.isDeleted());
            classSessionRepository.save(saved);
        }

        for (var row : payers) {
            var student = requireMapped(studentMap, row.getStudentId(), "student", "payer");
            var saved = payerRepository.save(Payer.builder()
                    .student(student)
                    .fullName(row.getFullName())
                    .phoneNumber(row.getPhoneNumber())
                    .note(row.getNote())
                    .build());
            saved.setDeleted(row.isDeleted());
            payerRepository.save(saved);
        }

        return DataImportResultResponse.builder()
                .students(students.size())
                .weeklySchedules(schedules.size())
                .packagePurchases(packages.size())
                .classSessions(sessions.size())
                .payers(payers.size())
                .build();
    }

    private void clearAllTables() {
        entityManager.createNativeQuery("DELETE FROM studio.class_sessions").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM studio.weekly_schedules").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM studio.payers").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM studio.package_purchases").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM studio.students").executeUpdate();
        entityManager.flush();
    }

    private long requireId(Long id, String entityName) {
        if (id == null) {
            throw new BadRequestException("Snapshot " + entityName + " row has null id");
        }
        return id;
    }

    private <T> T requireMapped(Map<Long, T> map, Long id, String parentName, String childName) {
        if (id == null) {
            throw new BadRequestException("Snapshot " + childName + " row has null " + parentName + "Id");
        }
        T value = map.get(id);
        if (value == null) {
            throw new BadRequestException("Snapshot " + childName + " references missing " + parentName + "Id=" + id);
        }
        return value;
    }

    private DataSnapshotResponse.StudentRow toStudentRow(Student s) {
        return DataSnapshotResponse.StudentRow.builder()
                .id(s.getId())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .phoneNumber(s.getPhoneNumber())
                .pricingType(s.getPricingType())
                .pricePerClass(s.getPricePerClass())
                .currency(s.getCurrency())
                .timezone(s.getTimezone())
                .classType(s.getClassType())
                .startDate(s.getStartDate())
                .holidayMode(s.isHolidayMode())
                .holidayFrom(s.getHolidayFrom())
                .holidayTo(s.getHolidayTo())
                .stoppedAttending(s.isStoppedAttending())
                .notes(s.getNotes())
                .debtor(s.isDebtor())
                .deleted(s.isDeleted())
                .build();
    }

    private DataSnapshotResponse.WeeklyScheduleRow toScheduleRow(WeeklySchedule s) {
        return DataSnapshotResponse.WeeklyScheduleRow.builder()
                .id(s.getId())
                .studentId(s.getStudent().getId())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .durationMinutes(s.getDurationMinutes())
                .effectiveFromEpochDay(s.getEffectiveFromEpochDay())
                .deleted(s.isDeleted())
                .build();
    }

    private DataSnapshotResponse.PackagePurchaseRow toPackageRow(PackagePurchase p) {
        return DataSnapshotResponse.PackagePurchaseRow.builder()
                .id(p.getId())
                .studentId(p.getStudent().getId())
                .totalClasses(p.getTotalClasses())
                .classesRemaining(p.getClassesRemaining())
                .amountPaid(p.getAmountPaid())
                .currency(p.getCurrency())
                .paymentDate(p.getPaymentDate())
                .description(p.getDescription())
                .deleted(p.isDeleted())
                .build();
    }

    private DataSnapshotResponse.ClassSessionRow toSessionRow(ClassSession s) {
        return DataSnapshotResponse.ClassSessionRow.builder()
                .id(s.getId())
                .studentId(s.getStudent().getId())
                .weeklyScheduleId(s.getWeeklySchedule() == null ? null : s.getWeeklySchedule().getId())
                .classDate(s.getClassDate())
                .startTime(s.getStartTime())
                .timezone(s.getTimezone())
                .durationMinutes(s.getDurationMinutes())
                .status(s.getStatus())
                .paymentStatus(s.getPaymentStatus())
                .priceCharged(s.getPriceCharged())
                .currency(s.getCurrency())
                .packagePurchaseId(s.getPackagePurchase() == null ? null : s.getPackagePurchase().getId())
                .oneOff(s.isOneOff())
                .note(s.getNote())
                .deleted(s.isDeleted())
                .build();
    }

    private DataSnapshotResponse.PayerRow toPayerRow(Payer p) {
        return DataSnapshotResponse.PayerRow.builder()
                .id(p.getId())
                .studentId(p.getStudent().getId())
                .fullName(p.getFullName())
                .phoneNumber(p.getPhoneNumber())
                .note(p.getNote())
                .deleted(p.isDeleted())
                .build();
    }
}

