package com.saas.system.domain.port.out.location;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.system.domain.model.location.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDepartmentRepositoryPort extends ICodeRepositoryPort<Department, UUID> {

    List<Department> findByCountryId(UUID countryId);

    Optional<Department> findByCountryIdAndCode(UUID countryId, String code);

    boolean existsByCountryIdAndCode(UUID countryId, String code);
}
