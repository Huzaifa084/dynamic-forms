package com.apex.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class PayRollApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayRollApplication.class, args);
    }
}
