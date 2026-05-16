package com.saas.system.domain.port.out.location;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.system.domain.model.location.Municipality;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMunicipalityRepositoryPort extends ICodeRepositoryPort<Municipality, UUID> {

    List<Municipality> findByDepartmentId(UUID departmentId);

    Optional<Municipality> findByDepartmentIdAndCode(UUID departmentId, String code);

    boolean existsByDepartmentIdAndCode(UUID departmentId, String code);
}
