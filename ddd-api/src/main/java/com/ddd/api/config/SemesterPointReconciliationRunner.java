package com.ddd.api.config;

import com.ddd.application.service.semesterpoint.impl.SemesterPointReconciliationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
@Profile("semester-point-reconcile")
@RequiredArgsConstructor
public class SemesterPointReconciliationRunner implements ApplicationRunner {
    private final SemesterPointReconciliationService reconciliation;
    private final ObjectMapper mapper;
    private final ConfigurableApplicationContext context;
    @Value("${points.reconcile.apply:false}") private boolean apply;
    @Value("${points.reconcile.allow-missing-ledger:false}") private boolean allowMissingLedger;
    @Value("${points.reconcile.output:semester-point-reconciliation.json}") private String output;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            var report=apply?reconciliation.repair(allowMissingLedger):reconciliation.preview();
            mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of(output).toFile(),report);
        } finally {
            context.close();
        }
    }
}
