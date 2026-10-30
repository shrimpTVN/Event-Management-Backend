package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Otp;
import com.ddd.infrastructure.entity.OtpJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OtpMapper {

    Otp toDomain(OtpJpaEntity entity);

    @Mapping(target ="isActive", source="active")
    OtpJpaEntity toEntity(Otp domain);

}
