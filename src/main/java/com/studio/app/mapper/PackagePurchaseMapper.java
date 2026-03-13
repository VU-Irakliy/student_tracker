package com.studio.app.mapper;

import com.studio.app.dto.response.PackagePurchaseResponse;
import com.studio.app.entity.PackagePurchase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for {@link PackagePurchase} → {@link PackagePurchaseResponse}.
 */
@Mapper(componentModel = "spring")
public interface PackagePurchaseMapper {

    @Mapping(target = "studentId",   source = "student.id")
    @Mapping(target = "studentName", expression = "java(pkg.getStudent().getFirstName() + \" \" + pkg.getStudent().getLastName())")
    @Mapping(target = "exhausted",   expression = "java(pkg.isExhausted())")
    @Mapping(target = "convertedAmountPaid", ignore = true)
    PackagePurchaseResponse toResponse(PackagePurchase pkg);

    List<PackagePurchaseResponse> toResponseList(List<PackagePurchase> packages);
}
