package com.erha.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.erha.ops", "com.erha.quote"})
@EntityScan(basePackages = {"com.erha.ops", "com.erha.quote"})
@EnableJpaRepositories(basePackages = {"com.erha.ops", "com.erha.quote"})
public class ErhaOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErhaOpsApplication.class, args);
    }
}