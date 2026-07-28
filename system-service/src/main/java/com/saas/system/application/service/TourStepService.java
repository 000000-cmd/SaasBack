package com.saas.system.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.model.TourStep;
import com.saas.system.domain.port.in.IMenuUseCase;
import com.saas.system.domain.port.in.ITourStepUseCase;
import com.saas.system.domain.port.out.ITourStepRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TourStepService extends GenericCrudService<TourStep, UUID> implements ITourStepUseCase {

    private final ITourStepRepositoryPort repo;
    private final IMenuUseCase menuUseCase;

    public TourStepService(ITourStepRepositoryPort repo, IMenuUseCase menuUseCase) {
        super(repo);
        this.repo = repo;
        this.menuUseCase = menuUseCase;
    }

    @Override protected String getResourceName() { return "TourStep"; }

    @Override
    protected void applyChanges(TourStep existing, TourStep incoming) {
        if (incoming.getTitle() != null)        existing.setTitle(incoming.getTitle());
        if (incoming.getBody() != null)         existing.setBody(incoming.getBody());
        if (incoming.getIcon() != null)         existing.setIcon(incoming.getIcon());
        if (incoming.getDisplayOrder() != null) existing.setDisplayOrder(incoming.getDisplayOrder());
        // menuId y anchor pueden venir explicitamente en null (desasociar el paso
        // o convertirlo en diapositiva), asi que se aplican siempre.
        existing.setMenuId(incoming.getMenuId());
        existing.setAnchor(incoming.getAnchor());
    }

    @Override
    protected void onBeforeCreate(TourStep step) {
        super.onBeforeCreate(step);
        validateTarget(step);
        validateMenuExists(step.getMenuId());
    }

    @Override
    protected void onBeforeUpdate(TourStep existing, TourStep incoming) {
        super.onBeforeUpdate(existing, incoming);
        validateTarget(incoming);
        validateMenuExists(incoming.getMenuId());
    }

    /**
     * Un paso no puede apuntar a dos sitios: el front resolveria por precedencia
     * y el admin no entenderia por que su ancla se ignora.
     */
    private void validateTarget(TourStep step) {
        if (step.getMenuId() != null && step.getAnchor() != null && !step.getAnchor().isBlank()) {
            throw new BusinessException("Un paso apunta a un menú o a un ancla, no a ambos");
        }
    }

    /**
     * Sin esto, un menuId inexistente revienta la FK al guardar y el handler
     * generico devuelve 500 con SQL crudo. Es config que edita un admin humano,
     * asi que debe fallar con 400 y mensaje claro.
     */
    private void validateMenuExists(UUID menuId) {
        if (menuId == null) return;
        try {
            menuUseCase.getById(menuId);
        } catch (ResourceNotFoundException e) {
            throw new BusinessException("El menú indicado no existe: " + menuId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourStep> getAllOrdered() {
        return repo.findAllOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourStep> getStepsForRoles(Set<UUID> roleIds) {
        Set<UUID> visibleMenus = menuUseCase.getMenusForRoles(roleIds).stream()
                .map(Menu::getId)
                .collect(Collectors.toSet());
        return repo.findAllOrdered().stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()))
                // Sin menu -> siempre visible. Con menu -> solo si el rol lo ve.
                .filter(s -> s.getMenuId() == null || visibleMenus.contains(s.getMenuId()))
                .toList();
    }
}
