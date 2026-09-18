package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.response.OwnerBusinessResponse;
import com.saas.business.domain.model.Branch;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessOwner;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.in.IBranchUseCase;
import com.saas.business.domain.port.in.IBusinessOwnerUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.application.service.PersonLookupService;
import com.saas.business.application.service.WhatsappLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints S2S (no traversed por gateway). Resuelve el businessId del dueño a
 * partir de su userId, para que el auth lo selle como claim en el JWT sin que
 * cada request downstream tenga que hacer el lookup (userId → persona →
 * business_owner → business).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalBusinessController {

    private final PersonLookupService personLookup;
    private final IBusinessOwnerUseCase ownerUseCase;
    private final IBusinessUseCase businessUseCase;
    private final IEmployeeUseCase employeeUseCase;
    private final IBranchUseCase branchUseCase;
    private final WhatsappLinkService whatsappLink;

    /**
     * El negocio de un usuario, sea DUEÑO o EMPLEADO.
     *
     * <p>Se sella como claim en el JWT, y desde ahí gobierna el aislamiento
     * entre negocios: toda petición acotada se comprueba contra este valor en
     * vez de creerse el que llegue por parámetro.</p>
     *
     * <p>Antes resolvía solo al dueño. Con la comprobación de aislamiento
     * activa eso dejaba a los EMPLEADOS sin claim, y un token sin claim pasa
     * cualquier identificador: justo el agujero que se está cerrando. Ahora se
     * prueban los dos caminos.</p>
     *
     * <p>Nunca falla: si el usuario no tiene negocio todavía (recién
     * registrado, sin aprovisionar) devuelve null y el token sale sin el
     * claim, igual que antes.</p>
     */
    /**
     * Las credenciales de WhatsApp de un negocio, para poder ENVIAR en su nombre.
     *
     * <p>Vive en {@code /internal} y no en la API publica por lo que devuelve:
     * un token que permite mandar mensajes como ese negocio. Solo lo llama
     * events-service, de servicio a servicio.</p>
     *
     * <p>El token NO viaja en el evento de Kafka. Un evento se guarda en el
     * topic, se reintenta y se registra: un secreto ahi dentro queda escrito en
     * sitios que nadie va a limpiar. Por eso el envio lo pregunta cuando le
     * toca enviar.</p>
     *
     * <p>Devuelve nulos cuando el negocio no tiene su propio numero, y entonces
     * el envio usa el de la plataforma. No es un error: es un negocio que
     * todavia no conecto el suyo.</p>
     */
    @GetMapping("/whatsapp-credentials")
    public WhatsappCredentials whatsappCredentials(@RequestParam UUID businessId) {
        WhatsappLinkService.Estado e = whatsappLink.estado(businessId);
        if (!e.conectado() || !e.enabled()) return new WhatsappCredentials(null, null);
        return new WhatsappCredentials(e.phoneNumberId(), whatsappLink.tokenDe(businessId));
    }

    public record WhatsappCredentials(String phoneNumberId, String accessToken) {}

    @GetMapping("/owner-business")
    public OwnerBusinessResponse ownerBusiness(@RequestParam UUID userId) {
        UUID businessId = personLookup.thirdPartyIdByUser(userId)
                .map(this::businessOfPerson)
                .orElse(null);
        return new OwnerBusinessResponse(businessId);
    }

    /** Primero como dueño; si no lo es, como empleado a través de su sede. */
    private UUID businessOfPerson(UUID thirdPartyId) {
        UUID asOwner = ownerUseCase.findByThirdParty(thirdPartyId).stream()
                .findFirst()
                .map(BusinessOwner::getBusinessId)
                .orElse(null);
        if (asOwner != null) return asOwner;

        return employeeUseCase.findByThirdParty(thirdPartyId).stream()
                .findFirst()
                .map(Employee::getBranchId)
                .map(branchId -> branchUseCase.getById(branchId))
                .map(Branch::getBusinessId)
                .orElse(null);
    }

    /** Solo el nombre: lo firma finance en los correos de liquidacion y nomina. */
    public record BusinessName(UUID id, String name) {}

    @GetMapping("/businesses/{id}/name")
    public BusinessName businessName(@PathVariable UUID id) {
        Business b = businessUseCase.getById(id);
        // El nombre comercial es el que el empleado reconoce; el juridico es el
        // respaldo si nunca se lleno.
        String label = b.getTradeName() != null && !b.getTradeName().isBlank()
                ? b.getTradeName() : b.getName();
        return new BusinessName(b.getId(), label);
    }
}
