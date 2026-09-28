package com.ddd.domain.repository;

import com.ddd.domain.model.Fanpage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public interface FanpageRepository {
    boolean existsByName(@NotNull @NotBlank String name);

    Fanpage save(Fanpage fanpage);

    Optional<Fanpage> findById(Long fanpageId);
}
