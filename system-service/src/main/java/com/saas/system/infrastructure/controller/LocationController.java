package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.application.dto.request.location.CountryRequest;
import com.saas.system.application.dto.request.location.DepartmentRequest;
import com.saas.system.application.dto.request.location.MunicipalityRequest;
import com.saas.system.application.dto.request.location.NeighborhoodRequest;
import com.saas.system.application.dto.response.location.CountryResponse;
import com.saas.system.application.dto.response.location.DepartmentResponse;
import com.saas.system.application.dto.response.location.MunicipalityResponse;
import com.saas.system.application.dto.response.location.NeighborhoodResponse;
import com.saas.system.application.service.location.CountryService;
import com.saas.system.application.service.location.DepartmentService;
import com.saas.system.application.service.location.MunicipalityService;
import com.saas.system.application.service.location.NeighborhoodService;
import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.model.location.Municipality;
import com.saas.system.domain.model.location.Neighborhood;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD de division politica: pais > departamento > municipio > barrio/vereda.
 *
 * Estos endpoints son la "fuente de verdad" (MySQL). Para busqueda full-text
 * con minimo 3 letras, ver {@code search-service /search/locations/*}.
 */
@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationController {

    private final CountryService countryService;
    private final DepartmentService departmentService;
    private final MunicipalityService municipalityService;
    private final NeighborhoodService neighborhoodService;

    // ==================== COUNTRY ====================

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> listCountries() {
        return ResponseEntity.ok(ApiResponse.success(
                countryService.getAll().stream().map(this::toCountryResponse).toList()));
    }

    @GetMapping("/countries/{id}")
    public ResponseEntity<ApiResponse<CountryResponse>> getCountry(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toCountryResponse(countryService.getById(id))));
    }

    @GetMapping("/countries/code/{code}")
    public ResponseEntity<ApiResponse<CountryResponse>> getCountryByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(toCountryResponse(countryService.getByCode(code))));
    }

    @PostMapping("/countries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> createCountry(@Valid @RequestBody CountryRequest req) {
        Country domain = Country.builder()
                .code(req.code()).name(req.name()).officialName(req.officialName())
                .isoCode3(req.isoCode3()).numericCode(req.numericCode())
                .phoneCode(req.phoneCode()).currencyCode(req.currencyCode())
                .currencySymbol(req.currencySymbol()).continent(req.continent())
                .build();
        Country saved = countryService.create(domain);
        return ResponseEntity.ok(ApiResponse.created(toCountryResponse(saved)));
    }

    @PutMapping("/countries/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> updateCountry(@PathVariable UUID id,
                                                                       @Valid @RequestBody CountryRequest req) {
        Country incoming = Country.builder()
                .code(req.code()).name(req.name()).officialName(req.officialName())
                .isoCode3(req.isoCode3()).numericCode(req.numericCode())
                .phoneCode(req.phoneCode()).currencyCode(req.currencyCode())
                .currencySymbol(req.currencySymbol()).continent(req.continent())
                .build();
        return ResponseEntity.ok(ApiResponse.success(toCountryResponse(countryService.update(id, incoming))));
    }

    @DeleteMapping("/countries/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCountry(@PathVariable UUID id) {
        countryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Pais deshabilitado"));
    }

    @PatchMapping("/countries/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleCountryEnabled(@PathVariable UUID id,
                                                                   @RequestParam("value") boolean value) {
        countryService.toggleEnabled(id, value);
        return ResponseEntity.ok(ApiResponse.success(null, value ? "Pais habilitado" : "Pais inhabilitado"));
    }

    // ==================== DEPARTMENT ====================

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> listDepartments(
            @RequestParam(required = false) UUID countryId) {
        List<Department> items = (countryId != null)
                ? departmentService.findByCountryId(countryId)
                : departmentService.getAll();
        return ResponseEntity.ok(ApiResponse.success(items.stream().map(this::toDepartmentResponse).toList()));
    }

    @GetMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toDepartmentResponse(departmentService.getById(id))));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(@Valid @RequestBody DepartmentRequest req) {
        Department d = Department.builder().code(req.code()).name(req.name()).countryId(req.countryId()).build();
        return ResponseEntity.ok(ApiResponse.created(toDepartmentResponse(departmentService.create(d))));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(@PathVariable UUID id,
                                                                            @Valid @RequestBody DepartmentRequest req) {
        Department d = Department.builder().code(req.code()).name(req.name()).countryId(req.countryId()).build();
        return ResponseEntity.ok(ApiResponse.success(toDepartmentResponse(departmentService.update(id, d))));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Departamento deshabilitado"));
    }

    @PatchMapping("/departments/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleDepartmentEnabled(@PathVariable UUID id,
                                                                      @RequestParam("value") boolean value) {
        departmentService.toggleEnabled(id, value);
        return ResponseEntity.ok(ApiResponse.success(null, value ? "Departamento habilitado" : "Departamento inhabilitado"));
    }

    // ==================== MUNICIPALITY ====================

    @GetMapping("/municipalities")
    public ResponseEntity<ApiResponse<List<MunicipalityResponse>>> listMunicipalities(
            @RequestParam(required = false) UUID departmentId) {
        List<Municipality> items = (departmentId != null)
                ? municipalityService.findByDepartmentId(departmentId)
                : municipalityService.getAll();
        return ResponseEntity.ok(ApiResponse.success(items.stream().map(this::toMunicipalityResponse).toList()));
    }

    @GetMapping("/municipalities/{id}")
    public ResponseEntity<ApiResponse<MunicipalityResponse>> getMunicipality(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toMunicipalityResponse(municipalityService.getById(id))));
    }

    @PostMapping("/municipalities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MunicipalityResponse>> createMunicipality(@Valid @RequestBody MunicipalityRequest req) {
        Municipality m = Municipality.builder().code(req.code()).name(req.name()).departmentId(req.departmentId()).build();
        return ResponseEntity.ok(ApiResponse.created(toMunicipalityResponse(municipalityService.create(m))));
    }

    @PutMapping("/municipalities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MunicipalityResponse>> updateMunicipality(@PathVariable UUID id,
                                                                                @Valid @RequestBody MunicipalityRequest req) {
        Municipality m = Municipality.builder().code(req.code()).name(req.name()).departmentId(req.departmentId()).build();
        return ResponseEntity.ok(ApiResponse.success(toMunicipalityResponse(municipalityService.update(id, m))));
    }

    @DeleteMapping("/municipalities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMunicipality(@PathVariable UUID id) {
        municipalityService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Municipio deshabilitado"));
    }

    @PatchMapping("/municipalities/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleMunicipalityEnabled(@PathVariable UUID id,
                                                                        @RequestParam("value") boolean value) {
        municipalityService.toggleEnabled(id, value);
        return ResponseEntity.ok(ApiResponse.success(null, value ? "Municipio habilitado" : "Municipio inhabilitado"));
    }

    // ==================== NEIGHBORHOOD ====================

    @GetMapping("/neighborhoods")
    public ResponseEntity<ApiResponse<List<NeighborhoodResponse>>> listNeighborhoods(
            @RequestParam(required = false) UUID municipalityId,
            @RequestParam(required = false) String type) {
        List<Neighborhood> items;
        if (municipalityId != null && type != null) {
            items = neighborhoodService.findByMunicipalityIdAndType(municipalityId, type);
        } else if (municipalityId != null) {
            items = neighborhoodService.findByMunicipalityId(municipalityId);
        } else {
            items = neighborhoodService.getAll();
        }
        return ResponseEntity.ok(ApiResponse.success(items.stream().map(this::toNeighborhoodResponse).toList()));
    }

    @GetMapping("/neighborhoods/{id}")
    public ResponseEntity<ApiResponse<NeighborhoodResponse>> getNeighborhood(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toNeighborhoodResponse(neighborhoodService.getById(id))));
    }

    @PostMapping("/neighborhoods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NeighborhoodResponse>> createNeighborhood(@Valid @RequestBody NeighborhoodRequest req) {
        Neighborhood n = Neighborhood.builder()
                .code(req.code()).name(req.name()).type(req.type()).municipalityId(req.municipalityId())
                .build();
        return ResponseEntity.ok(ApiResponse.created(toNeighborhoodResponse(neighborhoodService.create(n))));
    }

    @PutMapping("/neighborhoods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NeighborhoodResponse>> updateNeighborhood(@PathVariable UUID id,
                                                                                @Valid @RequestBody NeighborhoodRequest req) {
        Neighborhood n = Neighborhood.builder()
                .code(req.code()).name(req.name()).type(req.type()).municipalityId(req.municipalityId())
                .build();
        return ResponseEntity.ok(ApiResponse.success(toNeighborhoodResponse(neighborhoodService.update(id, n))));
    }

    @DeleteMapping("/neighborhoods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNeighborhood(@PathVariable UUID id) {
        neighborhoodService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Barrio/Vereda deshabilitado"));
    }

    @PatchMapping("/neighborhoods/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleNeighborhoodEnabled(@PathVariable UUID id,
                                                                        @RequestParam("value") boolean value) {
        neighborhoodService.toggleEnabled(id, value);
        return ResponseEntity.ok(ApiResponse.success(null, value ? "Barrio/Vereda habilitado" : "Barrio/Vereda inhabilitado"));
    }

    // ==================== MAPPERS LOCALES ====================

    private CountryResponse toCountryResponse(Country c) {
        return new CountryResponse(c.getId(), c.getCode(), c.getName(), c.getOfficialName(),
                c.getIsoCode3(), c.getNumericCode(), c.getPhoneCode(), c.getCurrencyCode(),
                c.getCurrencySymbol(), c.getContinent(),
                c.getEnabled(), c.getVisible(), c.getCreatedDate(), c.getAuditDate());
    }

    private DepartmentResponse toDepartmentResponse(Department d) {
        Country country = d.getCountryId() != null
                ? safeGetCountry(d.getCountryId()) : null;
        return new DepartmentResponse(d.getId(), d.getCode(), d.getName(),
                d.getCountryId(),
                country != null ? country.getCode() : null,
                country != null ? country.getName() : null,
                d.getEnabled(), d.getVisible(), d.getCreatedDate(), d.getAuditDate());
    }

    private MunicipalityResponse toMunicipalityResponse(Municipality m) {
        Department dept = m.getDepartmentId() != null
                ? safeGetDepartment(m.getDepartmentId()) : null;
        Country country = (dept != null && dept.getCountryId() != null)
                ? safeGetCountry(dept.getCountryId()) : null;
        return new MunicipalityResponse(m.getId(), m.getCode(), m.getName(),
                m.getDepartmentId(),
                dept != null ? dept.getCode() : null,
                dept != null ? dept.getName() : null,
                country != null ? country.getId() : null,
                country != null ? country.getCode() : null,
                country != null ? country.getName() : null,
                m.getEnabled(), m.getVisible(), m.getCreatedDate(), m.getAuditDate());
    }

    private NeighborhoodResponse toNeighborhoodResponse(Neighborhood n) {
        Municipality muni = n.getMunicipalityId() != null
                ? safeGetMunicipality(n.getMunicipalityId()) : null;
        Department dept = (muni != null && muni.getDepartmentId() != null)
                ? safeGetDepartment(muni.getDepartmentId()) : null;
        Country country = (dept != null && dept.getCountryId() != null)
                ? safeGetCountry(dept.getCountryId()) : null;
        return new NeighborhoodResponse(n.getId(), n.getCode(), n.getName(), n.getType(),
                n.getMunicipalityId(),
                muni != null ? muni.getCode() : null,
                muni != null ? muni.getName() : null,
                dept != null ? dept.getId() : null,
                dept != null ? dept.getCode() : null,
                dept != null ? dept.getName() : null,
                country != null ? country.getId() : null,
                country != null ? country.getCode() : null,
                country != null ? country.getName() : null,
                n.getEnabled(), n.getVisible(), n.getCreatedDate(), n.getAuditDate());
    }

    private Country safeGetCountry(UUID id) {
        try { return countryService.getById(id); } catch (ResourceNotFoundException e) { return null; }
    }

    private Department safeGetDepartment(UUID id) {
        try { return departmentService.getById(id); } catch (ResourceNotFoundException e) { return null; }
    }

    private Municipality safeGetMunicipality(UUID id) {
        try { return municipalityService.getById(id); } catch (ResourceNotFoundException e) { return null; }
    }
}
