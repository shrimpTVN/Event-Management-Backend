package com.ddd.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.ddd")
@EntityScan(basePackages = "com.ddd")
@ConfigurationPropertiesScan(basePackages = "com.ddd")
@SpringBootApplication(scanBasePackages = "com.ddd")
public class StartApplication {
    public static void main(String[] args) {
        System.out.print("Hello and welcome!");

        SpringApplication.run(StartApplication.class, args);
    }
}
