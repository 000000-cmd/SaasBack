package com.saas.system.application.service;

import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.in.IMenuUseCase;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión de Menús.
 */
@Service
public class MenuService extends GenericCrudService<Menu, String> implements IMenuUseCase {

    private final IMenuRepositoryPort menuRepository;

    public MenuService(IMenuRepositoryPort repository) {
        super(repository);
        this.menuRepository = repository;
    }

    @Override
    protected String getResourceName() {
        return "Menú";
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> getByParentId(String parentId) {
        return menuRepository.findByParentId(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> getRootMenus() {
        return menuRepository.findRootMenus();
    }
}