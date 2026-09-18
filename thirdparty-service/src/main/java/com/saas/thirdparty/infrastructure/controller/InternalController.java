package com.saas.thirdparty.infrastructure.controller;

import com.saas.thirdparty.application.dto.event.ThirdPartyEventPayload;
import com.saas.thirdparty.application.dto.response.BankAccountResponse;
import com.saas.thirdparty.application.dto.request.ThirdPartyRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyResponse;
import com.saas.thirdparty.application.mapper.ThirdPartyMapper;
import com.saas.thirdparty.application.service.ThirdPartyReindexPublisher;
import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.domain.port.in.IThirdPartyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Endpoints internos S2S (sin JWT; {@code /internal/**} permitido en SecurityConfig).
 * Consumidos por otros microservicios via Feign:
 *  - search-service: reindex-from-source (payload del tercero).
 *  - business-service: alta de la persona durante el aprovisionamiento del negocio.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final ThirdPartyReindexPublisher reindexPublisher;
    private final IThirdPartyUseCase useCase;
    private final ThirdPartyMapper mapper;
    private final com.saas.thirdparty.infrastructure.persistence.repository
            .JpaThirdPartyContactRepository contacts;
    private final com.saas.thirdparty.infrastructure.persistence.repository
            .JpaBankAccountRepository bankAccounts;
    private final com.saas.thirdparty.application.mapper.BankAccountMapper bankAccountMapper;

    @GetMapping("/third-parties/all")
    public List<ThirdPartyEventPayload> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        return reindexPublisher.buildPage(page, size);
    }

    @GetMapping("/third-parties/count")
    public Map<String, Long> count() {
        return Map.of("total", useCase.count());
    }

    /** Payload de UN tercero, para el reindex puntual desde la gestion de Elastic. */
    @GetMapping("/third-parties/one/{id}")
    public ThirdPartyEventPayload one(@PathVariable UUID id) {
        return reindexPublisher.buildOne(id);
    }

    /** Alta S2S de una persona (usada por el aprovisionamiento del negocio). */
    @PostMapping("/third-parties")
    public ThirdPartyResponse create(@Valid @RequestBody ThirdPartyRequest req) {
        return mapper.toResponse(useCase.create(mapper.toDomain(req)));
    }

    /**
     * Actualización S2S de una persona (el aprovisionamiento hace PUT cuando el
     * usuario ya tiene tercero, en vez de crear otro).
     */
    @PutMapping("/third-parties/{id}")
    public ThirdPartyResponse update(@PathVariable UUID id, @Valid @RequestBody ThirdPartyRequest req) {
        return mapper.toResponse(useCase.update(id, mapper.toDomain(req)));
    }

    /** Pre-check S2S de duplicado de documento (evita huerfanos en orquestaciones). */
    @GetMapping("/third-parties/document/exists")
    public Map<String, Boolean> documentExists(@RequestParam UUID documentTypeId,
                                               @RequestParam String documentNumber) {
        return Map.of("exists", useCase.existsByDocument(documentTypeId, documentNumber));
    }

    /** Nombres de personas en lote (id -> nombre completo). Para listas detalladas S2S. */
    @PostMapping("/third-parties/names")
    public Map<String, String> names(@RequestBody Set<UUID> ids) {
        Map<String, String> out = new HashMap<>();
        for (ThirdParty t : useCase.findByIds(ids)) {
            out.put(t.getId().toString(), fullName(t));
        }
        return out;
    }

    /**
     * Tarjetas de persona en lote (id -> nombre + foto). Para vistas que
     * muestran al empleado con su perfil (p.ej. compensación individual).
     */
    @PostMapping("/third-parties/cards")
    public Map<String, PersonCard> cards(@RequestBody Set<UUID> ids) {
        Map<String, PersonCard> out = new HashMap<>();
        for (ThirdParty t : useCase.findByIds(ids)) {
            out.put(t.getId().toString(), new PersonCard(fullName(t), t.getPhotoUrl()));
        }
        return out;
    }

    public record PersonCard(String fullName, String photoUrl) {}

    /**
     * Lo que hace falta para escribirle a alguien sobre su dinero: como se
     * llama, a que correo verificado y con que documento se abre el PDF que le
     * llega adjunto.
     */
    public record NotifyTarget(UUID thirdPartyId, String fullName, String documentNumber, String email) {}

    /**
     * Datos de notificacion en lote. Los pide finance al liquidar y al dispersar
     * nomina, en UNA llamada por corrida en vez de tres por empleado.
     *
     * <p>El correo solo sale si es PRINCIPAL y VERIFICADO: mandar el extracto de
     * sueldo de alguien a una direccion que nadie confirmo es lo unico peor que
     * no mandarlo. Quien no lo tenga viaja con {@code email} nulo y quien llama
     * decide (hoy: se omite el envio y queda en la bitacora).</p>
     */
    /**
     * Cuentas bancarias de varias personas, en lote.
     *
     * <p>Las pide finance al abrir el asistente de nomina: el dueño tiene que
     * ver a donde le va a consignar a cada uno SIN salir de la pantalla, que es
     * justo lo que antes le tocaba preguntar por WhatsApp mientras pagaba.</p>
     */
    @PostMapping("/third-parties/bank-accounts")
    public Map<String, List<BankAccountResponse>> bankAccounts(@RequestBody Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return bankAccountMapper
                .fromViews(bankAccounts.findViewByThirdParties(ids.stream().map(UUID::toString).toList()))
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.thirdPartyId().toString()));
    }

    @PostMapping("/third-parties/notify-targets")
    public Map<String, NotifyTarget> notifyTargets(@RequestBody Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();

        Map<String, String> emailByThirdParty = new HashMap<>();
        contacts.findPrimaryVerifiedByTypeCodeAndThirdParties(
                        "EMAIL", ids.stream().map(UUID::toString).toList())
                .forEach(c -> emailByThirdParty.put(c.getThirdPartyId(), c.getValue()));

        Map<String, NotifyTarget> out = new HashMap<>();
        for (ThirdParty t : useCase.findByIds(ids)) {
            String key = t.getId().toString();
            out.put(key, new NotifyTarget(t.getId(), fullName(t),
                    t.getDocumentNumber(), emailByThirdParty.get(key)));
        }
        return out;
    }

    private static String fullName(ThirdParty t) {
        return String.join(" ", Stream.of(t.getFirstName(), t.getSecondName(), t.getFirstLastName(), t.getSecondLastName())
                .filter(s -> s != null && !s.isBlank()).toList());
    }

    /** Resuelve la persona vinculada a una cuenta (para "mi empresa"). 404 si no existe. */
    @GetMapping("/third-parties/by-user/{userId}")
    public ResponseEntity<ThirdPartyResponse> byUser(@PathVariable UUID userId) {
        return useCase.findByUserId(userId)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Login flexible: resuelve la CUENTA (userId) dueña del numero de documento.
     * Solo mira personas ya vinculadas a un usuario; 404 si no hay match.
     */
    @GetMapping("/third-parties/user-by-document")
    public ResponseEntity<UserByDocument> userByDocument(@RequestParam String documentNumber) {
        return useCase.findAccountHolderByDocumentNumber(documentNumber.trim())
                .map(t -> new UserByDocument(t.getUserId()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record UserByDocument(UUID userId) {}

    // =================================================================
    // CONTACTOS — los consume events-service para el lanzamiento global y
    // para la verificacion por codigo. Van por /internal porque son S2S: el
    // gateway no expone /internal/**.
    // =================================================================

    /** Un destino del lanzamiento global: a quien, que contacto y su valor. */
    public record ContactTarget(UUID thirdPartyId, UUID contactId, String value) {}

    /**
     * Contactos PRINCIPALES Y VERIFICADOS de un tipo. Es la audiencia real de un
     * lanzamiento global: quien no tiene ese medio verificado sencillamente no
     * esta aqui, y quien llama lo registra como omitido con su motivo.
     */
    @GetMapping("/contacts/primary-verified")
    public List<ContactTarget> primaryVerified(@RequestParam String typeCode) {
        return contacts.findPrimaryVerifiedByTypeCode(typeCode).stream()
                .map(c -> new ContactTarget(
                        UUID.fromString(c.getThirdPartyId()),
                        UUID.fromString(c.getContactId()),
                        c.getValue()))
                .toList();
    }

    /** Cuantos recibirian por este medio, para el recuento previo al lanzamiento. */
    @GetMapping("/contacts/count")
    public Map<String, Long> countPrimaryVerified(@RequestParam String typeCode) {
        return Map.of("count", contacts.countPrimaryVerifiedByTypeCode(typeCode));
    }

    public record ContactDetail(UUID id, UUID thirdPartyId, String value,
                                String typeCode, Boolean isVerified) {}

    /** Detalle de un contacto: quien llama necesita su valor y su tipo para mandarle el codigo. */
    @GetMapping("/contacts/{id}")
    public ResponseEntity<ContactDetail> contact(@PathVariable UUID id) {
        return contacts.findById(id)
                .map(c -> new ContactDetail(c.getId(), c.getThirdPartyId(), c.getValue(),
                        contacts.findTypeCodeByContactId(id.toString()), c.getIsVerified()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Marca el contacto como verificado. Lo llama events-service cuando alguien
     * acierta su codigo; la validacion del codigo vive alli, aqui solo se sella
     * el resultado.
     */
    @PutMapping("/contacts/{id}/verify")
    public ResponseEntity<ContactDetail> verifyContact(@PathVariable UUID id) {
        return contacts.findById(id)
                .map(c -> {
                    c.setIsVerified(Boolean.TRUE);
                    c.setVerifiedAt(java.time.LocalDateTime.now());
                    contacts.save(c);
                    return new ContactDetail(c.getId(), c.getThirdPartyId(), c.getValue(),
                            contacts.findTypeCodeByContactId(id.toString()), Boolean.TRUE);
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
