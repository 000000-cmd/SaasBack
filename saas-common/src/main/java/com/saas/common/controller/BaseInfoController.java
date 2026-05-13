package com.saas.common.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.dto.ServiceInfoDTO;
import com.saas.common.dto.ServiceInfoDTO.DependencyStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controlador base reutilizable por cada microservicio para exponer su info
 * de runtime (/api/info) y un endpoint legacy de version (/api/version).
 *
 * Resolucion de version (en orden):
 *   1. {@link BuildProperties} desde META-INF/build-info.properties (lo genera
 *      el goal build-info de spring-boot-maven-plugin durante mvn package).
 *   2. Variable de entorno APP_VERSION (override en deployment).
 *   3. Lectura directa del &lt;version&gt; del parent pom desde disco. Util
 *      cuando se ejecuta desde IntelliJ sin pasar por mvn package.
 *   4. "unknown".
 *
 * El estado y las dependencias se calculan a partir del HealthEndpoint nativo
 * de Actuator, mapeando los indicadores conocidos (db, redis, kafka,
 * discoveryComposite/eureka) a entradas tipadas para el front.
 */
public abstract class BaseInfoController {

    @Value("${spring.application.name:unknown}")
    protected String applicationName;

    @Value("${spring.profiles.active:default}")
    protected String activeProfile;

    @Value("${APP_VERSION:}")
    protected String envVersion;

    @Autowired(required = false)
    protected BuildProperties buildProperties;

    @Autowired(required = false)
    protected HealthEndpoint healthEndpoint;

    @GetMapping("/api/info")
    public ResponseEntity<ApiResponse<ServiceInfoDTO>> getServiceInfo() {
        return ResponseEntity.ok(ApiResponse.success(buildServiceInfo()));
    }

    @GetMapping("/api/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        Map<String, String> version = new HashMap<>();
        version.put("service", applicationName);
        version.put("version", getServiceVersion());
        version.put("environment", activeProfile);
        return ResponseEntity.ok(version);
    }

    protected ServiceInfoDTO buildServiceInfo() {
        Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put("os.name", System.getProperty("os.name"));
        additionalInfo.put("os.arch", System.getProperty("os.arch"));

        List<DependencyStatus> deps = collectDependencies();
        String aggregateStatus = aggregateStatus(deps);

        return ServiceInfoDTO.builder()
                .serviceName(applicationName)
                .version(getServiceVersion())
                .environment(activeProfile)
                .status(aggregateStatus)
                .javaVersion(System.getProperty("java.version"))
                .springBootVersion(SpringBootVersion.getVersion())
                .uptimeMillis(ManagementFactory.getRuntimeMXBean().getUptime())
                .buildTime(getBuildTime())
                .dependencies(deps)
                .additionalInfo(additionalInfo)
                .build();
    }

    protected String getServiceVersion() {
        if (buildProperties != null && buildProperties.getVersion() != null) {
            return buildProperties.getVersion();
        }
        if (envVersion != null && !envVersion.isBlank()) {
            return envVersion;
        }
        // Fallback dev: ejecutamos desde target/classes (IntelliJ) y aun no se
        // genero META-INF/build-info.properties. Leemos directo del parent pom.
        String fromPom = readVersionFromParentPom();
        if (fromPom != null) return fromPom;
        return "unknown";
    }

    /**
     * Cache + busqueda del &lt;version&gt; del parent pom. Sube directorio por
     * directorio desde {@code user.dir} buscando un pom.xml con
     * {@code <packaging>pom</packaging>}; ese es el agregador del monorepo.
     */
    private static volatile String CACHED_POM_VERSION;

    private static String readVersionFromParentPom() {
        if (CACHED_POM_VERSION != null) return CACHED_POM_VERSION.isEmpty() ? null : CACHED_POM_VERSION;
        synchronized (BaseInfoController.class) {
            if (CACHED_POM_VERSION != null) return CACHED_POM_VERSION.isEmpty() ? null : CACHED_POM_VERSION;
            String found = lookupParentPomVersion();
            CACHED_POM_VERSION = found == null ? "" : found;
            return found;
        }
    }

    /** {@code <version>X</version>} que aparece DESPUES de cerrar la etiqueta parent. */
    private static final Pattern PROJECT_VERSION_AFTER_PARENT = Pattern.compile(
            "</parent>[\\s\\S]*?<version>\\s*([^<\\s]+)\\s*</version>", Pattern.DOTALL);
    /** Primera {@code <version>} del documento, fallback cuando no hay parent. */
    private static final Pattern FIRST_VERSION = Pattern.compile(
            "<version>\\s*([^<\\s]+)\\s*</version>", Pattern.DOTALL);

    private static String lookupParentPomVersion() {
        Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParent()) {
            Path pom = dir.resolve("pom.xml");
            if (!Files.isRegularFile(pom)) continue;
            try {
                String xml = stripComments(Files.readString(pom));
                if (!xml.contains("<packaging>pom</packaging>")) continue;

                Matcher m = PROJECT_VERSION_AFTER_PARENT.matcher(xml);
                if (m.find()) return m.group(1).trim();

                m = FIRST_VERSION.matcher(xml);
                if (m.find()) return m.group(1).trim();
            } catch (IOException ignore) {
                // sigue subiendo
            }
        }
        return null;
    }

    private static String stripComments(String xml) {
        return xml.replaceAll("(?s)<!--.*?-->", "");
    }

    protected LocalDateTime getBuildTime() {
        if (buildProperties == null) return null;
        Instant t = buildProperties.getTime();
        return t == null ? null : LocalDateTime.ofInstant(t, ZoneId.systemDefault());
    }

    /** Mapea HealthEndpoint -> dependencias tipadas para el front. */
    protected List<DependencyStatus> collectDependencies() {
        List<DependencyStatus> out = new ArrayList<>();
        if (healthEndpoint == null) return out;

        HealthComponent root = healthEndpoint.health();
        if (!(root instanceof CompositeHealth composite)) return out;

        composite.getComponents().forEach((name, comp) -> {
            String type = classify(name);
            if (type == null) return; // ignoramos diskSpace, ping, livenessState, etc.
            String status = comp.getStatus() == null ? "UNKNOWN" : comp.getStatus().getCode();
            String detail = null;
            if (comp instanceof Health h && !"UP".equalsIgnoreCase(status)) {
                Object err = h.getDetails().get("error");
                if (err != null) detail = err.toString();
            }
            out.add(DependencyStatus.builder()
                    .name(name)
                    .type(type)
                    .status(status)
                    .detail(detail)
                    .build());
        });
        return out;
    }

    /** Mapea el nombre del HealthIndicator a un tipo conocido. null = ignorar. */
    protected String classify(String name) {
        String n = name.toLowerCase();
        if (n.contains("db") || n.equals("datasource") || n.contains("mysql")) return "DB";
        if (n.contains("kafka")) return "KAFKA";
        if (n.contains("eureka") || n.contains("discoverycomposite") || n.contains("discoveryclient")) return "EUREKA";
        if (n.contains("redis")) return "REDIS";
        if (n.contains("elasticsearch") || n.contains("elastic")) return "ELASTIC";
        return null;
    }

    protected String aggregateStatus(List<DependencyStatus> deps) {
        if (deps.isEmpty()) return Status.UP.getCode();
        long down = deps.stream().filter(d -> "DOWN".equalsIgnoreCase(d.getStatus())).count();
        if (down == 0) return Status.UP.getCode();
        if (down == deps.size()) return Status.DOWN.getCode();
        return "DEGRADED";
    }
}
