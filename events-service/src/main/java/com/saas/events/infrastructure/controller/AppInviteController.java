package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.application.service.DispatchService;
import com.saas.events.infrastructure.client.ThirdPartyInternalClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Mándale el enlace de la app a mi equipo".
 *
 * <p>Hasta ahora la unica forma de repartir el APK era copiar un enlace y
 * pegarlo en WhatsApp uno por uno. Esto lo manda por correo a las personas que
 * el dueno elija, con el enlace que siempre apunta a la version vigente.</p>
 *
 * <p>Va por el mismo camino que cualquier otro aviso: se resuelve una
 * notificacion, se renderiza su plantilla y se despacha. Como se pasa el
 * tercero, ademas queda en SU BANDEJA — asi que quien ya tenga la app instalada
 * y solo necesite el enlace tambien lo encuentra dentro.</p>
 */
@Slf4j
@RestController
@RequestMapping("/notification/app-invite")
@RequiredArgsConstructor
public class AppInviteController {

    private static final String CODIGO = "APP_INVITE";

    private final DispatchService dispatch;
    private final ThirdPartyInternalClient thirdParty;

    public record InviteRequest(
            @NotEmpty List<UUID> thirdPartyIds,
            @NotBlank String link,
            @NotBlank String businessName) {}

    public record InviteResult(int sent, int withoutEmail, List<String> missing) {}

    @PostMapping
    public ResponseEntity<ApiResponse<InviteResult>> invite(@Valid @RequestBody InviteRequest req) {
        Set<UUID> pedidos = Set.copyOf(req.thirdPartyIds());

        // ponytail: se trae la lista completa de correos principales verificados
        // y se filtra aqui. A la escala de un negocio (decenas de personas) es
        // una llamada y sobra; el dia que sean miles, el cambio es un endpoint
        // en thirdparty que reciba los ids.
        List<ThirdPartyInternalClient.ContactTarget> correos =
                thirdParty.primaryVerified("EMAIL").stream()
                        .filter(c -> pedidos.contains(c.thirdPartyId()))
                        .toList();

        Map<String, String> datos = Map.of(
                "NEGOCIO", req.businessName(),
                "LINK", req.link());

        int enviados = 0;
        for (ThirdPartyInternalClient.ContactTarget c : correos) {
            // Uno por uno y no en bloque: asi cada persona recibe SU correo (no
            // una copia con todos los destinatarios a la vista) y ademas le
            // queda la entrada en su propia bandeja.
            dispatch.dispatch(CODIGO, List.of(c.value()), datos, null, c.thirdPartyId());
            enviados++;
        }

        // Quien no tiene correo principal verificado no recibe. Se dice cuantos
        // son: la diferencia entre "no le llego" y "no sabemos" es justamente
        // esto, y sin el numero el dueno cree que fueron todos.
        List<String> sinCorreo = pedidos.stream()
                .filter(id -> correos.stream().noneMatch(c -> c.thirdPartyId().equals(id)))
                .map(UUID::toString)
                .toList();

        log.info("Invitacion a la app: {} enviadas, {} sin correo verificado", enviados, sinCorreo.size());
        return ResponseEntity.ok(ApiResponse.success(
                new InviteResult(enviados, sinCorreo.size(), sinCorreo)));
    }
}
