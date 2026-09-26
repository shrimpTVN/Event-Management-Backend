package com.ddd.domain.repository;

import com.ddd.domain.model.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MajorRepository {
    Major findById(Long id);

    List<Major> findAll();

    List<Major> findBySchoolId(Long schoolId);

    Major saveMajor(Major major);

    Major updateMajor(Long id, Major major);

    void deleteMajor(Long id);
}
