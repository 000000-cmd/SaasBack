package sss.thirdpartyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "sss.thirdpartyservice",
        "com.saas.common"
})
@EnableDiscoveryClient
@EnableScheduling
@EntityScan(basePackages = {
        "sss.thirdpartyservice",
        "com.saas.common.outbox"
})
@EnableJpaRepositories(basePackages = {
        "sss.thirdpartyservice",
        "com.saas.common.outbox"
})
public class ThirdpartyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThirdpartyServiceApplication.class, args);
    }

}
