package com.studio.app.service.impl;

import com.studio.app.dto.request.WeeklyScheduleRequest;
import com.studio.app.dto.response.WeeklyScheduleResponse;
import com.studio.app.entity.WeeklySchedule;
import com.studio.app.exception.BadRequestException;
import com.studio.app.exception.ConflictException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.StudentMapper;
import com.studio.app.repository.StudentRepository;
import com.studio.app.repository.WeeklyScheduleRepository;
import com.studio.app.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Default implementation of {@link ScheduleService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final WeeklyScheduleRepository scheduleRepository;
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    /** {@inheritDoc} */
    @Override
    public WeeklyScheduleResponse addSchedule(Long studentId, WeeklyScheduleRequest request) {
        var student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        if (student.isStoppedAttending()) {
            throw new BadRequestException("Cannot add schedules for a student who stopped attending");
        }

        if (scheduleRepository.existsByStudentIdAndDayOfWeekAndDeletedFalse(studentId, request.getDayOfWeek())) {
            throw new ConflictException("Student already has a schedule on " + request.getDayOfWeek());
        }

        var schedule = WeeklySchedule.builder()
                .student(student)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .durationMinutes(request.getDurationMinutes())
                .effectiveFromEpochDay(student.getStartDate() == null
                        ? LocalDate.now().toEpochDay()
                        : student.getStartDate().toEpochDay())
                .build();

        return studentMapper.toWeeklyScheduleResponse(scheduleRepository.save(schedule));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<WeeklyScheduleResponse> getSchedulesForStudent(Long studentId) {
        studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        return scheduleRepository.findByStudentIdAndDeletedFalse(studentId)
                .stream()
                .map(studentMapper::toWeeklyScheduleResponse)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public WeeklyScheduleResponse updateSchedule(Long studentId, Long scheduleId, WeeklyScheduleRequest request) {
        var schedule = scheduleRepository.findByIdAndStudentIdAndDeletedFalse(scheduleId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("WeeklySchedule", scheduleId));

        if (schedule.getStudent().isStoppedAttending()) {
            throw new BadRequestException("Cannot update schedules for a student who stopped attending");
        }

        var dayChanged = !schedule.getDayOfWeek().equals(request.getDayOfWeek());
        if (dayChanged && scheduleRepository.existsByStudentIdAndDayOfWeekAndDeletedFalse(studentId, request.getDayOfWeek())) {
            throw new ConflictException("Student already has a schedule on " + request.getDayOfWeek());
        }

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setDurationMinutes(request.getDurationMinutes());

        return studentMapper.toWeeklyScheduleResponse(scheduleRepository.save(schedule));
    }

    /** {@inheritDoc} */
    @Override
    public void removeSchedule(Long studentId, Long scheduleId) {
        var schedule = scheduleRepository.findByIdAndStudentIdAndDeletedFalse(scheduleId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("WeeklySchedule", scheduleId));
        schedule.setDeleted(true);
        scheduleRepository.save(schedule);
    }
}
