package com.saas.system.application.service.location;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.application.dto.event.location.LocationReindexPayload;
import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.model.location.Municipality;
import com.saas.system.domain.model.location.Neighborhood;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de soporte para reindex desde search-service. Resuelve los 4
 * niveles de location y los devuelve denormalizados (cada nivel arrastra la
 * cadena padre completa) para que el reindex en elastic sea single-pass.
 *
 * <p>Si un padre no se encuentra (raro, integridad referencial), los campos
 * quedan null en vez de tronar; el indexador downstream tolera nulls.
 */
@Service
@RequiredArgsConstructor
public class LocationReindexService {

    private static final String SEP = ", ";

    private final CountryService countryService;
    private final DepartmentService departmentService;
    private final MunicipalityService municipalityService;
    private final NeighborhoodService neighborhoodService;

    // ==================== COUNT ====================

    public long countCountries()       { return countryService.count(); }
    public long countDepartments()     { return departmentService.count(); }
    public long countMunicipalities()  { return municipalityService.count(); }
    public long countNeighborhoods()   { return neighborhoodService.count(); }

    // ==================== PAGINADO ====================

    public List<LocationReindexPayload> findCountries(int page, int size) {
        return countryService.findAllPaged(page, size).stream().map(this::toCountryPayload).toList();
    }

    public List<LocationReindexPayload> findDepartments(int page, int size) {
        return departmentService.findAllPaged(page, size).stream().map(this::toDepartmentPayload).toList();
    }

    public List<LocationReindexPayload> findMunicipalities(int page, int size) {
        return municipalityService.findAllPaged(page, size).stream().map(this::toMunicipalityPayload).toList();
    }

    public List<LocationReindexPayload> findNeighborhoods(int page, int size) {
        return neighborhoodService.findAllPaged(page, size).stream().map(this::toNeighborhoodPayload).toList();
    }

    // ==================== MAPPERS ====================

    private LocationReindexPayload toCountryPayload(Country c) {
        return new LocationReindexPayload(
                c.getId(), "PAIS", c.getEnabled(),
                c.getId(), c.getCode(), c.getName(),
                null, null, null,
                null, null, null,
                null, null, null,
                null,
                c.getName(),
                c.getName()
        );
    }

    private LocationReindexPayload toDepartmentPayload(Department d) {
        Country country = safe(() -> countryService.getById(d.getCountryId()));
        String countryCode = country != null ? country.getCode() : null;
        String countryName = country != null ? country.getName() : null;
        return new LocationReindexPayload(
                d.getId(), "DEPARTAMENTO", d.getEnabled(),
                d.getCountryId(), countryCode, countryName,
                d.getId(), d.getCode(), d.getName(),
                null, null, null,
                null, null, null,
                null,
                join(d.getName(), countryName),
                join(countryName, d.getName())
        );
    }

    private LocationReindexPayload toMunicipalityPayload(Municipality m) {
        Department dept = safe(() -> departmentService.getById(m.getDepartmentId()));
        Country country = dept != null ? safe(() -> countryService.getById(dept.getCountryId())) : null;
        String deptCode = dept != null ? dept.getCode() : null;
        String deptName = dept != null ? dept.getName() : null;
        UUID countryId = country != null ? country.getId() : null;
        String countryCode = country != null ? country.getCode() : null;
        String countryName = country != null ? country.getName() : null;
        return new LocationReindexPayload(
                m.getId(), "MUNICIPIO", m.getEnabled(),
                countryId, countryCode, countryName,
                m.getDepartmentId(), deptCode, deptName,
                m.getId(), m.getCode(), m.getName(),
                null, null, null,
                null,
                join(m.getName(), deptName, countryName),
                join(countryName, deptName, m.getName())
        );
    }

    private LocationReindexPayload toNeighborhoodPayload(Neighborhood n) {
        Municipality muni = safe(() -> municipalityService.getById(n.getMunicipalityId()));
        Department dept = muni != null ? safe(() -> departmentService.getById(muni.getDepartmentId())) : null;
        Country country = dept != null ? safe(() -> countryService.getById(dept.getCountryId())) : null;
        String muniCode = muni != null ? muni.getCode() : null;
        String muniName = muni != null ? muni.getName() : null;
        UUID deptId = dept != null ? dept.getId() : null;
        String deptCode = dept != null ? dept.getCode() : null;
        String deptName = dept != null ? dept.getName() : null;
        UUID countryId = country != null ? country.getId() : null;
        String countryCode = country != null ? country.getCode() : null;
        String countryName = country != null ? country.getName() : null;
        return new LocationReindexPayload(
                n.getId(), n.getType() != null ? n.getType() : "BARRIO", n.getEnabled(),
                countryId, countryCode, countryName,
                deptId, deptCode, deptName,
                muni != null ? muni.getId() : null, muniCode, muniName,
                n.getId(), n.getCode(), n.getName(),
                n.getType(),
                join(n.getName(), muniName, deptName, countryName),
                join(countryName, deptName, muniName, n.getName())
        );
    }

    // ---------- helpers ----------

    private static <T> T safe(java.util.function.Supplier<T> get) {
        try { return get.get(); } catch (ResourceNotFoundException ex) { return null; }
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(SEP);
            sb.append(p);
        }
        return sb.toString();
    }
}
