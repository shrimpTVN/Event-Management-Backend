package com.ddd.infrastructure.mapper;
import com.ddd.domain.model.Major;
import com.ddd.infrastructure.entity.MajorJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")

public interface MajorMapper {
    Major toDomainModel(MajorJpaEntity entity);
    MajorJpaEntity toJpaEntity(Major domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "school", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDomain(Major major, @MappingTarget MajorJpaEntity entity);
}