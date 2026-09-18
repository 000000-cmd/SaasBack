package com.saas.thirdparty.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.service.GenericCrudService;
import com.saas.thirdparty.domain.model.AccountKind;
import com.saas.thirdparty.domain.model.BankAccount;
import com.saas.thirdparty.domain.port.in.IBankAccountUseCase;
import com.saas.thirdparty.domain.port.out.IBankAccountRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Cuentas bancarias de una persona.
 *
 * <p>Dos reglas y las dos van AQUI y no en la pantalla, porque las mismas
 * cuentas se dan de alta desde la web, desde el alta del empleado en el movil y
 * (a futuro) desde cualquier importacion:</p>
 *
 * <ol>
 *   <li>Una cuenta esta completa o no existe: BANK exige banco, tipo y numero;
 *       BREV exige la llave. Media cuenta es una transferencia que rebota.</li>
 *   <li>Siempre hay exactamente una principal. La primera lo es sola, y marcar
 *       otra apaga la anterior en la misma transaccion.</li>
 * </ol>
 */
@Slf4j
@Service
public class BankAccountService extends GenericCrudService<BankAccount, UUID>
        implements IBankAccountUseCase {

    private final IBankAccountRepositoryPort repo;

    public BankAccountService(IBankAccountRepositoryPort repo) {
        super(repo);
        this.repo = repo;
    }

    @Override protected String getResourceName() { return "Cuenta bancaria"; }

    @Override
    protected void onBeforeCreate(BankAccount account) {
        validate(account);
        normalize(account);

        // La primera cuenta de alguien es su principal sin preguntar: obligar a
        // marcarla seria un paso extra cuya unica respuesta posible es "si".
        boolean first = repo.countByThirdParty(account.getThirdPartyId()) == 0;
        boolean wantsPrimary = Boolean.TRUE.equals(account.getIsPrimary());
        if (first || wantsPrimary) {
            if (!first) repo.clearPrimary(account.getThirdPartyId());
            account.setIsPrimary(Boolean.TRUE);
        } else {
            account.setIsPrimary(Boolean.FALSE);
        }
    }

    @Override
    protected void applyChanges(BankAccount existing, BankAccount incoming) {
        if (incoming.getAccountKind() != null)   existing.setAccountKind(incoming.getAccountKind());
        if (incoming.getAlias() != null)         existing.setAlias(incoming.getAlias());

        // Los campos de la forma se copian SIEMPRE, incluso a null: al pasar de
        // BANK a BREV hay que limpiar banco/tipo/numero, y con la copia
        // condicional se quedarian pegados datos de la forma anterior.
        existing.setBankId(incoming.getBankId());
        existing.setAccountType(incoming.getAccountType());
        existing.setAccountNumber(incoming.getAccountNumber());
        existing.setBrevKey(incoming.getBrevKey());

        validate(existing);
        normalize(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> findByThirdParty(UUID thirdPartyId) {
        return repo.findByThirdParty(thirdPartyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> findByThirdParties(Collection<UUID> thirdPartyIds) {
        return repo.findByThirdParties(thirdPartyIds);
    }

    @Override
    @Transactional
    public BankAccount makePrimary(UUID id) {
        BankAccount account = getById(id);
        if (Boolean.TRUE.equals(account.getIsPrimary())) return account;
        repo.clearPrimary(account.getThirdPartyId());
        account.setIsPrimary(Boolean.TRUE);
        return repo.update(account);
    }

    /**
     * Borrar la principal deja a la persona sin cuenta por defecto. En vez de
     * prohibirlo, la siguiente hereda el puesto: prohibirlo obligaria a crear la
     * nueva antes de poder quitar una que quiza ya esta cerrada.
     */
    @Override
    protected void onAfterDelete(UUID id, BankAccount snapshot) {
        if (!Boolean.TRUE.equals(snapshot.getIsPrimary())) return;
        repo.findByThirdParty(snapshot.getThirdPartyId()).stream()
                .filter(a -> !a.getId().equals(id))
                .findFirst()
                .ifPresent(next -> {
                    next.setIsPrimary(Boolean.TRUE);
                    repo.update(next);
                    log.info("Cuenta {} pasa a principal tras borrar la anterior", next.getId());
                });
    }

    private void validate(BankAccount a) {
        if (a.getAccountKind() == null) {
            throw new BusinessException("Indica si es una cuenta de banco o una llave BREV");
        }
        if (a.getAccountKind() == AccountKind.BREV) {
            if (isBlank(a.getBrevKey())) throw new BusinessException("Escribe la llave BREV");
            return;
        }
        if (a.getBankId() == null) throw new BusinessException("Elige el banco");
        if (isBlank(a.getAccountType())) throw new BusinessException("Indica si es de ahorros o corriente");
        if (isBlank(a.getAccountNumber())) throw new BusinessException("Escribe el número de cuenta");
    }

    /** Deja solo los campos de la forma elegida, para no arrastrar basura. */
    private void normalize(BankAccount a) {
        if (a.getAccountKind() == AccountKind.BREV) {
            a.setBankId(null);
            a.setAccountType(null);
            a.setAccountNumber(null);
            // La llave NO se toca: ni trim, ni mayusculas, ni quitar puntos. Es
            // literalmente lo que el banco resuelve.
        } else {
            a.setBrevKey(null);
            if (a.getAccountNumber() != null) a.setAccountNumber(a.getAccountNumber().trim());
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
