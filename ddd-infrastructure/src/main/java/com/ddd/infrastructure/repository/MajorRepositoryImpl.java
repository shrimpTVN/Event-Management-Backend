package com.ddd.infrastructure.repository;
import com.ddd.domain.model.Major;
import com.ddd.domain.model.School;
import com.ddd.domain.repository.MajorRepository;
import com.ddd.infrastructure.entity.MajorJpaEntity;
import com.ddd.infrastructure.entity.SchoolJpaEntity;
import com.ddd.infrastructure.mapper.MajorMapper;
import com.ddd.infrastructure.mapper.SchoolMapper;
import com.ddd.infrastructure.repository.jpaRepository.MajorJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.SchoolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@RequiredArgsConstructor
public class MajorRepositoryImpl implements MajorRepository {

    private final MajorJpaRepository majorJpaRepository;
    private final MajorMapper majorMapper;
    private final SchoolMapper schoolMapper;
    private final SchoolJpaRepository schoolJpaRepository;

    @Override
    public Major findById(Long id) {
        MajorJpaEntity entity = majorJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Major not found with id: " + id));
        return majorMapper.toDomainModel(entity);
    }
    @Override
    public List<Major> findAll() {

        return majorJpaRepository.findAll()
                .stream()
                .map(majorMapper::toDomainModel)
                .toList();

    }
    @Override
    public List<Major> findBySchoolId(Long schoolId) {
        return majorJpaRepository.findBySchoolIdAndIsActiveTrue(schoolId)
                .stream().map(majorMapper::toDomainModel).toList();
    }
    @Override
    public Major saveMajor(Major major) {
        MajorJpaEntity entity = majorMapper.toJpaEntity(major);
        return majorMapper.toDomainModel(majorJpaRepository.save(entity));
    }
    @Override
    public Major updateMajor(Long id, Major major) {
        MajorJpaEntity entity = majorJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Major not found"));

        majorMapper.updateEntityFromDomain(major, entity);

        SchoolJpaEntity schoolEntity = schoolJpaRepository.findById(major.getSchoolId())
                .orElseThrow(() -> new RuntimeException("School not found "));

        entity.setSchool(schoolEntity);

        return majorMapper.toDomainModel(majorJpaRepository.save(entity));
    }
    @Override
    public void deleteMajor(Long id) {
        MajorJpaEntity entity = majorJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Major not found"));
        entity.setIsActive(false);
        majorJpaRepository.save(entity);
    }
}
