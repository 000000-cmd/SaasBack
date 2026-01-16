package com.saas.systemservice.application.services.menu;

import com.saas.systemservice.application.services.GenericCrudService;
import com.saas.systemservice.domain.model.menu.Menu;
import com.saas.systemservice.domain.ports.in.menu.IMenuUseCase;
import com.saas.systemservice.domain.ports.out.menu.IMenuRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class MenuService extends GenericCrudService<Menu, String> implements IMenuUseCase {

    public MenuService(IMenuRepositoryPort repository) {
        super(repository);
    }

}
