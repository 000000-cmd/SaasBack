package com.saas.thirdparty.infrastructure.persistence.repository;

import com.saas.thirdparty.infrastructure.persistence.entity.BankAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBankAccountRepository extends JpaRepository<BankAccountEntity, UUID> {

    List<BankAccountEntity> findByThirdPartyIdOrderByIsPrimaryDescCreatedDateAsc(UUID thirdPartyId);

    List<BankAccountEntity> findByThirdPartyIdInOrderByIsPrimaryDesc(Collection<UUID> thirdPartyIds);

    long countByThirdPartyId(UUID thirdPartyId);

    /**
     * Baja la bandera de principal de todas las cuentas de una persona.
     *
     * <p>Va como UPDATE masivo y no leyendo-y-guardando una a una porque el
     * UNIQUE de la columna generada rechaza que existan dos principales ni
     * siquiera un instante: hay que apagar la anterior ANTES de encender la
     * nueva, en la misma transaccion.</p>
     */
    @Modifying
    @Query("UPDATE BankAccountEntity b SET b.isPrimary = false "
         + "WHERE b.thirdPartyId = :thirdPartyId AND b.isPrimary = true")
    int clearPrimary(@Param("thirdPartyId") UUID thirdPartyId);

    /**
     * Las cuentas con el NOMBRE del banco ya resuelto.
     *
     * <p>Consulta nativa con join al catalogo porque {@code bank} vive en esta
     * misma base aunque lo gobierne system-service: pedir los nombres por HTTP
     * para luego cruzarlos aqui seria un salto de red por nada. Es el mismo
     * motivo por el que los contactos resuelven asi su tipo.</p>
     */
    @Query(value = """
            SELECT ba.Id AS id, ba.ThirdPartyId AS thirdPartyId, ba.AccountKind AS accountKind,
                   ba.BankId AS bankId, b.Name AS bankName, ba.AccountType AS accountType,
                   ba.AccountNumber AS accountNumber, ba.BrevKey AS brevKey,
                   ba.Alias AS alias, ba.IsPrimary AS isPrimary
            FROM bank_account ba
            LEFT JOIN bank b ON b.Id = ba.BankId
            WHERE ba.ThirdPartyId IN (:thirdPartyIds)
              AND ba.Visible = 1 AND ba.Enabled = 1
            ORDER BY ba.IsPrimary DESC, ba.CreatedDate ASC
            """, nativeQuery = true)
    List<AccountView> findViewByThirdParties(@Param("thirdPartyIds") Collection<String> thirdPartyIds);

    /**
     * El CATALOGO de bancos, para que la app pueda ofrecerlo al dar de alta una
     * cuenta.
     *
     * <p>Vive aqui y no en system-service porque {@code bank} no esta dado de
     * alta como catalogo dinamico: {@code /system/list/bank} responde 404, y sin
     * lista el desplegable salia vacio — no se podia elegir banco y por tanto no
     * se podia registrar donde cobrar. Este servicio ya es el dueño de
     * {@code bank_account} y ya cruza esta tabla para componer la etiqueta, asi
     * que exponerla de solo lectura no le añade responsabilidades.</p>
     */
    @Query(value = """
            SELECT b.Id AS id, b.Code AS code, b.Name AS name
            FROM bank b
            WHERE b.Visible = 1 AND b.Enabled = 1
            ORDER BY b.Name ASC
            """, nativeQuery = true)
    List<BankView> findBanks();

    /** Lo minimo para pintar un desplegable: que mostrar y que guardar. */
    interface BankView {
        String getId();
        String getCode();
        String getName();
    }

    /** Proyeccion de la consulta anterior. */
    interface AccountView {
        String getId();
        String getThirdPartyId();
        String getAccountKind();
        String getBankId();
        String getBankName();
        String getAccountType();
        String getAccountNumber();
        String getBrevKey();
        String getAlias();
        Boolean getIsPrimary();
    }
}
