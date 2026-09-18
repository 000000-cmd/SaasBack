package com.saas.system.application.service.flow;

import com.saas.common.exception.BusinessException;
import com.saas.system.application.dto.response.flow.ResolvedFlowView;
import com.saas.system.domain.model.flow.Flow;
import com.saas.system.domain.model.flow.FlowChannel;
import com.saas.system.domain.model.flow.FlowControl;
import com.saas.system.domain.model.flow.FlowField;
import com.saas.system.domain.model.flow.FlowSection;
import com.saas.system.domain.port.in.IRolePermissionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * El flujo listo para que lo pinte quien sea: web, panel, APK o el bot.
 *
 * <p>Devuelve solo lo activo, solo lo de ese canal, y solo los botones que
 * quien pregunta puede usar. El filtrado va aqui y no en cada cliente: si cada
 * front decidiera por su cuenta que esconder, el APK y WhatsApp tendrian que
 * reimplementar la misma regla, y al que se equivoque le sale un boton que no
 * deberia existir.</p>
 *
 * <p>Los permisos se resuelven contra {@code role_permission}, el mismo
 * mecanismo que ya alimenta los menus. Este modulo NO trae permisos propios.</p>
 */
@Service
@RequiredArgsConstructor
public class FlowResolverService {

    private final FlowSnapshotCache cache;
    private final IRolePermissionUseCase rolePermissions;

    @Transactional(readOnly = true)
    public ResolvedFlowView resolve(String code, String channelRaw, Set<String> roles) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Falta el código del flujo");
        }
        FlowChannel canal = FlowChannel.of(channelRaw);
        FlowSnapshotCache.Snapshot snap = cache.forChannel(code.trim().toUpperCase(), canal);

        Flow flujo = snap.flow();
        if (Boolean.FALSE.equals(flujo.getEnabled())) {
            throw new BusinessException("El flujo " + flujo.getCode() + " está desactivado");
        }

        Set<String> permisos = roles == null || roles.isEmpty()
                ? Set.of()
                : rolePermissions.getPermissionCodesByRoleCodes(roles);

        List<ResolvedFlowView.Section> secciones = new ArrayList<>();
        int paso = 1;
        for (FlowSnapshotCache.SectionSnapshot s : snap.sections()) {
            secciones.add(toView(s, paso++, permisos));
        }

        return new ResolvedFlowView(flujo.getCode(), flujo.getName(), flujo.getDescription(),
                canal.name(), secciones);
    }

    /**
     * El numero de paso se calcula al vuelo y no se guarda.
     *
     * <p>Guardarlo obligaria a renumerar todas las secciones cada vez que se
     * desactiva una, y bastaria con olvidarse una vez para que el flujo dijera
     * "paso 3 de 5" con cuatro pasos. Aqui es siempre la posicion real dentro
     * de lo que este canal va a ver.</p>
     */
    private ResolvedFlowView.Section toView(FlowSnapshotCache.SectionSnapshot s, int paso,
                                            Set<String> permisos) {
        FlowSection sec = s.section();

        List<ResolvedFlowView.Field> campos = s.fields().stream()
                .map(FlowResolverService::toView)
                .toList();

        List<ResolvedFlowView.Control> botones = s.controls().stream()
                .filter(c -> puede(c, permisos))
                .map(c -> new ResolvedFlowView.Control(
                        c.getCode(), c.getLabel(),
                        c.getAction() == null ? "CUSTOM" : c.getAction().name(),
                        c.getVariant()))
                .toList();

        return new ResolvedFlowView.Section(
                sec.getCode(), sec.getName(), sec.getDescription(), paso,
                sec.getKind() == null ? "FORM" : sec.getKind().name(),
                campos, botones);
    }

    /** Sin permiso declarado, cualquiera que llegue a la pantalla puede usarlo. */
    private boolean puede(FlowControl c, Set<String> permisos) {
        return c.getPermissionCode() == null || c.getPermissionCode().isBlank()
                || permisos.contains(c.getPermissionCode());
    }

    private static ResolvedFlowView.Field toView(FlowField f) {
        return new ResolvedFlowView.Field(
                f.getCode(), f.getLabel(), f.getPlaceholder(), f.getHelpText(),
                f.getDataType(), f.getMaskCode(), f.getSourceKey(),
                Boolean.TRUE.equals(f.getIsRequired()), f.getWidth());
    }
}
