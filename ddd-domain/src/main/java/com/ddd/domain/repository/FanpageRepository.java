package com.ddd.domain.repository;

import com.ddd.domain.model.Fanpage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface FanpageRepository {
    boolean existsByName(@NotNull @NotBlank String name);

    Fanpage save(Fanpage fanpage);
}
