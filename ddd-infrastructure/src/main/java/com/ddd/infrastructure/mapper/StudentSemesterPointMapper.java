package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.infrastructure.entity.StudentSemesterPointJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentSemesterPointMapper {

    @Mapping(target="userId", source="user.id")
    @Mapping(target="semesterId", source="semester.id")
    @Mapping(target="pointCategoryId", source="pointCategory.id")
    StudentSemesterPoint toDomain(StudentSemesterPointJpaEntity entity);

    StudentSemesterPointJpaEntity toJpaEntity(StudentSemesterPoint domain);

    List<StudentSemesterPoint> toDomainList(List<StudentSemesterPointJpaEntity> entities);
    List<StudentSemesterPointJpaEntity> toJpaEntityList(List<StudentSemesterPoint> domains);
}
