package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.PointTransaction;
import com.ddd.infrastructure.entity.PointTransactionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointTransactionMapper {

    @Mapping(target= "id", source= "user.id")
    @Mapping(target= "semesterId", source= "semester.id")
    @Mapping(target= "pointCategoryId", source= "pointCategory.id")
    @Mapping(target= "eventId", source= "event.id")
    PointTransaction toDomain(PointTransactionJpaEntity pointTransactionJpaEntity);


    PointTransactionJpaEntity toJpaEntity(PointTransaction pointTransaction);
}
