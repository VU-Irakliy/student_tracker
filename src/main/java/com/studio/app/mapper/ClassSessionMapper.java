package com.studio.app.mapper;

import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.entity.ClassSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for {@link ClassSession} → {@link ClassSessionResponse}.
 */
@Mapper(componentModel = "spring")
public interface ClassSessionMapper {

    @Mapping(target = "studentId",       source = "student.id")
    @Mapping(target = "studentName",     expression = "java(session.getStudent().getFirstName() + \" \" + session.getStudent().getLastName())")
    @Mapping(target = "packagePurchaseId", source = "packagePurchase.id")
    @Mapping(target = "timezone", source = "timezone")
    @Mapping(target = "paymentDateTime", source = "paymentDateTime")
    @Mapping(target = "convertedPrices", ignore = true)
    @Mapping(target = "originalClassDate", ignore = true)
    @Mapping(target = "originalStartTime", ignore = true)
    @Mapping(target = "originalTimezone", ignore = true)
    @Mapping(target = "viewerTimezone", ignore = true)
    ClassSessionResponse toResponse(ClassSession session);

    List<ClassSessionResponse> toResponseList(List<ClassSession> sessions);
}
