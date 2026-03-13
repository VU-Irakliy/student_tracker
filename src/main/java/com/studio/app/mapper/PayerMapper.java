package com.studio.app.mapper;

import com.studio.app.dto.response.PayerResponse;
import com.studio.app.entity.Payer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for {@link Payer} → {@link PayerResponse}.
 */
@Mapper(componentModel = "spring")
public interface PayerMapper {

    @Mapping(target = "studentId",   source = "student.id")
    @Mapping(target = "studentName", expression = "java(payer.getStudent().getFirstName() + \" \" + payer.getStudent().getLastName())")
    PayerResponse toResponse(Payer payer);

    List<PayerResponse> toResponseList(List<Payer> payers);
}
