package com.studio.app.mapper;

import com.studio.app.dto.response.PayerResponse;
import com.studio.app.dto.response.StudentResponse;
import com.studio.app.dto.response.WeeklyScheduleResponse;
import com.studio.app.entity.Payer;
import com.studio.app.entity.Student;
import com.studio.app.entity.WeeklySchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting {@link Student} and related entities
 * to their corresponding response DTOs.
 */
@Mapper(componentModel = "spring")
public interface StudentMapper {

    /**
     * Maps a {@link Student} to a {@link StudentResponse}.
     */
    @Mapping(target = "fullName", expression = "java(student.getFirstName() + \" \" + student.getLastName())")
    @Mapping(target = "weeklySchedules", source = "weeklySchedules")
    @Mapping(target = "payers", source = "payers")
    @Mapping(target = "classType", source = "classType")
    @Mapping(target = "convertedPrices", ignore = true)
    StudentResponse toResponse(Student student);

    List<StudentResponse> toResponseList(List<Student> students);

    /** Maps a {@link WeeklySchedule} to a {@link WeeklyScheduleResponse}. */
    @Mapping(target = "studentId", source = "student.id")
    WeeklyScheduleResponse toWeeklyScheduleResponse(WeeklySchedule schedule);

    /** Maps a {@link Payer} to a {@link PayerResponse}. */
    @Mapping(target = "studentId",   source = "student.id")
    @Mapping(target = "studentName", expression = "java(payer.getStudent().getFirstName() + \" \" + payer.getStudent().getLastName())")
    PayerResponse toPayerResponse(Payer payer);
}
