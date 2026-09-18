package com.saas.events.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.service.CodeCrudService;
import com.saas.events.domain.model.NotificationParameter;
import com.saas.events.domain.port.in.INotificationParameterUseCase;
import com.saas.events.domain.port.out.INotificationParameterRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class NotificationParameterService
        extends CodeCrudService<NotificationParameter, UUID>
        implements INotificationParameterUseCase {

    /**
     * Mismo patron que reconoce TemplateRenderer. Se valida aqui para que no se
     * pueda crear un parametro que el renderizador jamas encontraria: un codigo
     * en minusculas quedaria en la lista de la interfaz y nunca sustituiria.
     */
    private static final Pattern VALID_CODE = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    public NotificationParameterService(INotificationParameterRepositoryPort repo) { super(repo); }

    @Override protected String getResourceName() { return "Parámetro de notificación"; }

    @Override
    protected void onBeforeCreate(NotificationParameter entity) {
        validateCode(entity.getCode());
        super.onBeforeCreate(entity);
    }

    @Override
    protected void onBeforeUpdate(NotificationParameter existing, NotificationParameter incoming) {
        if (incoming.getCode() != null) validateCode(incoming.getCode());
        super.onBeforeUpdate(existing, incoming);
    }

    private void validateCode(String code) {
        if (code == null || !VALID_CODE.matcher(code).matches()) {
            throw new BusinessException(
                    "El código debe ir en mayúsculas, empezar por letra y usar solo letras, números y guion bajo. Ejemplo: VALOR_TOTAL");
        }
    }

    @Override
    protected void applyChanges(NotificationParameter existing, NotificationParameter incoming) {
        if (incoming.getCode() != null)        existing.setCode(incoming.getCode());
        if (incoming.getName() != null)        existing.setName(incoming.getName());
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
    }
}
