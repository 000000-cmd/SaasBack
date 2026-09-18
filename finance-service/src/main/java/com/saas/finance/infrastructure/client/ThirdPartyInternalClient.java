package com.saas.finance.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Datos de las personas, que viven en {@code saas_db} bajo thirdparty-service.
 * Finance guarda el {@code thirdPartyId} en el saldo, pero no el nombre ni el
 * correo: duplicarlos seria tener dos verdades.
 */
@FeignClient(name = "thirdparty-service", contextId = "thirdparty-for-finance", path = "/thirdparty")
public interface ThirdPartyInternalClient {

    record NotifyTarget(UUID thirdPartyId, String fullName, String documentNumber, String email) {}

    /**
     * Nombre, documento y correo verificado de varias personas de una vez.
     * En lote porque una dispersion de nomina toca a todo el equipo: una llamada
     * por empleado convertiria el pago en una tormenta de peticiones.
     */
    @PostMapping("/internal/third-parties/notify-targets")
    Map<String, NotifyTarget> notifyTargets(@RequestBody Set<UUID> thirdPartyIds);

    /**
     * Cuenta de una persona, con su etiqueta ya compuesta.
     * {@code label} es lo que acaba impreso en el comprobante.
     */
    record BankAccountView(UUID id, UUID thirdPartyId, String accountKind, String bankName,
                           String accountType, String accountNumber, String brevKey,
                           String alias, Boolean isPrimary, String label) {}

    /**
     * Cuentas de varias personas, agrupadas por tercero. Las pide el asistente
     * de nomina para que el dueño tenga a donde consignar SIN salir de la
     * pantalla.
     */
    @PostMapping("/internal/third-parties/bank-accounts")
    Map<String, List<BankAccountView>> bankAccounts(@RequestBody Set<UUID> thirdPartyIds);
}
