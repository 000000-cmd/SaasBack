package com.saas.systemservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;


@SpringBootApplication(scanBasePackages = {
        "com.saas.systemservice",
        "com.saas.saascommon"
})
@EntityScan(basePackages = {
        "com.saas.systemservice.infrastructure.adapters.out.persistence.entity",
        "com.saas.saascommon.persistence"
})
public class SystemServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemServiceApplication.class, args);
    }
}