package com.ddd.infrastructure.config.audit;

import com.ddd.domain.model.port.security.SecurityContextPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("auditorAwareImpl")
@RequiredArgsConstructor
public class AuditorAwareImpl  implements AuditorAware<Long> {
    private final SecurityContextPort securityContextPort;

    @Override
    public Optional<Long> getCurrentAuditor() {
        if (!securityContextPort.isAuthenticated()){
            return Optional.empty();
        }

        Long userId = securityContextPort.getCurrentUserId();
        return Optional.ofNullable(userId);
    }
}
