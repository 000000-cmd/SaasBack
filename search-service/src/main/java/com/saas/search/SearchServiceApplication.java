package com.saas.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Read model del sistema. Consume eventos de Kafka (topic {@code domain.events})
 * y mantiene indices de Elasticsearch para busquedas y agregaciones rapidas.
 *
 * <p><b>NO escribe a MySQL.</b> Por eso excluye {@code DataSourceAutoConfiguration}
 * en properties. La unica fuente de verdad es Elasticsearch (read model).
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.search",
        "com.saas.common"
})
@EnableDiscoveryClient
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
