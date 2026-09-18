package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.AppointmentNotice;
import com.saas.business.domain.port.out.IAppointmentNoticeRepositoryPort;
import com.saas.business.infrastructure.persistence.repository.JpaAppointmentNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppointmentNoticeRepositoryAdapter implements IAppointmentNoticeRepositoryPort {

    private final JpaAppointmentNoticeRepository repo;

    /**
     * {@code @Transactional} aqui y no en quien llama: un {@code @Modifying}
     * sin transaccion falla con "Executing an update/delete query", y eso paso
     * de verdad — al agendar funcionaba (heredaba la transaccion de la reserva)
     * y desde el barrido, que no tiene ninguna, reventaba en silencio. La
     * necesidad es de la operacion, asi que va con la operacion.
     *
     * <p>{@code REQUIRED}: dentro de la transaccion de la reserva se une a
     * ella, que es lo correcto — si la cita no se guarda, la marca tampoco.</p>
     */
    @Override
    @Transactional
    public boolean markIfFirst(AppointmentNotice notice) {
        return repo.insertIfAbsent(UUID.randomUUID().toString(),
                notice.getAppointmentId().toString(),
                notice.getNotificationCode()) == 1;
    }
}
