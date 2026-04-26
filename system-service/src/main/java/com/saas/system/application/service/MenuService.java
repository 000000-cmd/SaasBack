package com.saas.system.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.service.CodeCrudService;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.in.IMenuUseCase;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MenuService extends CodeCrudService<Menu, UUID> implements IMenuUseCase {

    private final IMenuRepositoryPort menuRepo;

    public MenuService(IMenuRepositoryPort repo) {
        super(repo);
        this.menuRepo = repo;
    }

    @Override protected String getResourceName() { return "Menu"; }

    @Override
    protected void applyChanges(Menu existing, Menu incoming) {
        if (incoming.getCode() != null)         existing.setCode(incoming.getCode());
        if (incoming.getName() != null)         existing.setName(incoming.getName());
        if (incoming.getIcon() != null)         existing.setIcon(incoming.getIcon());
        if (incoming.getRoute() != null)        existing.setRoute(incoming.getRoute());
        if (incoming.getDisplayOrder() != null) existing.setDisplayOrder(incoming.getDisplayOrder());
        // parentId puede ser explicitamente null (mover a root) -> siempre lo aplico
        existing.setParentId(incoming.getParentId());
    }

    @Override
    protected void onBeforeCreate(Menu menu) {
        super.onBeforeCreate(menu);
        validateParent(menu, null);
    }

    @Override
    protected void onBeforeUpdate(Menu existing, Menu incoming) {
        super.onBeforeUpdate(existing, incoming);
        validateParent(incoming, existing.getId());
    }

    private void validateParent(Menu menu, UUID currentId) {
        if (menu.getParentId() == null) return;
        if (menu.getParentId().equals(currentId)) {
            throw new BusinessException("Un menu no puede ser padre de si mismo");
        }
        if (!menuRepo.existsById(menu.getParentId())) {
            throw new BusinessException("Padre no existe: " + menu.getParentId());
        }
    }

    @Override @Transactional(readOnly = true) public List<Menu> getRootMenus()           { return menuRepo.findRootMenus(); }
    @Override @Transactional(readOnly = true) public List<Menu> getChildren(UUID parent) { return menuRepo.findByParentId(parent); }
    @Override @Transactional(readOnly = true) public List<Menu> getMenusForRoles(Set<UUID> ids) {
        return menuRepo.findByRoleIds(ids);
    }
}
