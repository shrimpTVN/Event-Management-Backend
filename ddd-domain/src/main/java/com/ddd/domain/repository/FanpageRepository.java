package com.ddd.domain.repository;

import com.ddd.domain.model.Fanpage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

public interface FanpageRepository {
    boolean existsByName(@NotNull @NotBlank String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Fanpage save(Fanpage fanpage);

    Optional<Fanpage> findById(Long fanpageId);

    Optional<Fanpage> findByIdAndIsActive(Long fanpageId, boolean isActive);

    List<Fanpage> findAllByIdsAndIsActive(List<Long> fanpageIds, boolean isActive);
}
