package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Otp;
import com.ddd.infrastructure.entity.OtpJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OtpMapper {

    Otp toDomain(OtpJpaEntity entity);

    OtpJpaEntity toEntity(Otp domain);

}
