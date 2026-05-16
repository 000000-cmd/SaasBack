package com.saas.search.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign client a system-service via Eureka.
 *
 * <p>El atributo {@code path = "/system"} corresponde al
 * {@code server.servlet.context-path} de system-service.
 */
@FeignClient(
        name = "system-service",
        contextId = "system-internal-search",
        path = "/system"
)
public interface SystemInternalClient {

    // -------- roles --------
    @GetMapping("/internal/roles/all")
    List<JsonNode> fetchRoles(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/internal/roles/count")
    Map<String, Long> countRoles();

    // -------- locations (4 niveles, mismo alias 'locations' en ES) --------
    @GetMapping("/internal/locations/countries/all")
    List<JsonNode> fetchCountries(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/internal/locations/countries/count")
    Map<String, Long> countCountries();

    @GetMapping("/internal/locations/departments/all")
    List<JsonNode> fetchDepartments(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/internal/locations/departments/count")
    Map<String, Long> countDepartments();

    @GetMapping("/internal/locations/municipalities/all")
    List<JsonNode> fetchMunicipalities(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/internal/locations/municipalities/count")
    Map<String, Long> countMunicipalities();

    @GetMapping("/internal/locations/neighborhoods/all")
    List<JsonNode> fetchNeighborhoods(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/internal/locations/neighborhoods/count")
    Map<String, Long> countNeighborhoods();
}
