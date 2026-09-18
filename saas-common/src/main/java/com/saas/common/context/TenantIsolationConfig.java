package com.saas.common.context;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el aislamiento entre negocios en TODOS los servicios de una vez.
 *
 * <p>Vive en {@code com.saas.common}, que ya escanean los once
 * {@code @SpringBootApplication} del proyecto, asi que no hay nada que
 * enchufar servicio por servicio — y por tanto no hay forma de olvidarse de
 * uno, que es como se abren los agujeros de este tipo.</p>
 *
 * <p>Se excluye {@code /actuator/**}: son las sondas de salud de Docker y del
 * gateway, sin sesion ni negocio.</p>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TenantIsolationConfig implements WebMvcConfigurer {

    @Bean
    public TenantIsolationInterceptor tenantIsolationInterceptor() {
        return new TenantIsolationInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantIsolationInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/api/info", "/api/version");
    }
}
