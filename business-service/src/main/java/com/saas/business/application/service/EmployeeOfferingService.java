package com.saas.business.application.service;

import com.saas.business.domain.model.EmployeeOffering;
import com.saas.business.domain.port.in.IEmployeeOfferingUseCase;
import com.saas.business.domain.port.out.IEmployeeOfferingRepositoryPort;
import com.saas.business.infrastructure.persistence.repository.JpaEmployeeOfferingRepository;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EmployeeOfferingService
        extends GenericCrudService<EmployeeOffering, UUID>
        implements IEmployeeOfferingUseCase {

    private final IEmployeeOfferingRepositoryPort repo;
    private final JpaEmployeeOfferingRepository jpa;

    public EmployeeOfferingService(IEmployeeOfferingRepositoryPort repo,
                                   JpaEmployeeOfferingRepository jpa) {
        super(repo);
        this.repo = repo;
        this.jpa = jpa;
    }

    @Override protected String getResourceName() { return "Servicio del empleado"; }

    @Override protected void applyChanges(EmployeeOffering e, EmployeeOffering i) {
        if (i.getOfferingId() != null) e.setOfferingId(i.getOfferingId());
        // Nulables a proposito: null significa "vuelve a heredar del servicio",
        // y eso hay que poder guardarlo.
        e.setDurationMinutes(i.getDurationMinutes());
        e.setCommissionRate(i.getCommissionRate());
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeOffering> findByEmployee(UUID employeeId) {
        return repo.findByEmployeeId(employeeId);
    }

    /**
     * Guarda de golpe los servicios de un empleado: la pantalla es una lista de
     * casillas y lo que llega es el estado final, no un diff.
     *
     * <p>Lo que se quita se borra logicamente; lo que se anade puede estar ya
     * ahi borrado de antes, asi que se revive en vez de insertarse (ver
     * {@code upsertActive}: la clave unica no mira Visible).</p>
     */
    @Override @Transactional
    public List<EmployeeOffering> replaceForEmployee(UUID employeeId, List<EmployeeOffering> desired) {
        Set<UUID> queridos = new HashSet<>();
        for (EmployeeOffering d : desired) {
            queridos.add(d.getOfferingId());
            int tocadas = jpa.upsertActive(employeeId.toString(), d.getOfferingId().toString(),
                    d.getDurationMinutes(), d.getCommissionRate());
            if (tocadas == 0) {
                d.setEmployeeId(employeeId);
                d.setId(null);
                repo.save(d);
            }
        }
        for (EmployeeOffering actual : repo.findByEmployeeId(employeeId)) {
            if (!queridos.contains(actual.getOfferingId())) {
                repo.softDeleteById(actual.getId());
            }
        }
        return repo.findByEmployeeId(employeeId);
    }
}
