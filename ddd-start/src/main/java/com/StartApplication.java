package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StartApplication {
    public static void main(String[] args) {
        System.out.print("Hello and welcome!");

        SpringApplication.run(StartApplication.class, args);
    }
}
