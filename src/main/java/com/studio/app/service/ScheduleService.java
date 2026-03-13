package com.studio.app.service;

import com.studio.app.dto.request.WeeklyScheduleRequest;
import com.studio.app.dto.response.WeeklyScheduleResponse;

import java.util.List;

/**
 * Service interface for managing recurring weekly schedules.
 */
public interface ScheduleService {

    /**
     * Adds a new recurring weekly slot for a student.
     *
     * @param studentId the student ID
     * @param request   the schedule details
     * @return the created schedule entry
     */
    WeeklyScheduleResponse addSchedule(Long studentId, WeeklyScheduleRequest request);

    /**
     * Returns all active weekly schedule entries for a student.
     *
     * @param studentId the student ID
     * @return list of schedule responses
     */
    List<WeeklyScheduleResponse> getSchedulesForStudent(Long studentId);

    /**
     * Updates an existing weekly schedule slot.
     *
     * @param studentId  the student ID
     * @param scheduleId the schedule entry ID
     * @param request    the updated details
     * @return the updated schedule entry
     */
    WeeklyScheduleResponse updateSchedule(Long studentId, Long scheduleId, WeeklyScheduleRequest request);

    /**
     * Soft-deletes a recurring schedule entry.
     *
     * @param studentId  the student ID
     * @param scheduleId the schedule entry ID
     */
    void removeSchedule(Long studentId, Long scheduleId);
}
