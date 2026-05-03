package com.saas.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableScheduling
@EnableFeignClients(basePackages = "com.saas.auth.infrastructure.client")
@ComponentScan(basePackages = {
        "com.saas.auth",
        "com.saas.common"
})
@EntityScan(basePackages = {
        "com.saas.auth",
        "com.saas.common.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.saas.auth",
        "com.saas.common.outbox"
})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
