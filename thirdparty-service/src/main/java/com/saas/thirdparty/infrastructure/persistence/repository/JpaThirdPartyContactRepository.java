package com.saas.thirdparty.infrastructure.persistence.repository;

import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaThirdPartyContactRepository extends JpaRepository<ThirdPartyContactEntity, UUID> {
    List<ThirdPartyContactEntity> findByThirdPartyId(UUID thirdPartyId);

    /**
     * Contactos PRINCIPALES Y VERIFICADOS de un tipo, para el lanzamiento global.
     *
     * Consulta nativa con join al catalogo porque {@code contact_type} vive en
     * esta misma base ({@code saas_db}) aunque lo gobierne system-service: pedir
     * el id del tipo por HTTP para luego filtrar aqui seria un salto de red por
     * nada. Se filtra por el CODIGO y no por el id para que quien llama no tenga
     * que conocer UUID sembrados.
     */
    @Query(value = """
            SELECT c.ThirdPartyId AS thirdPartyId, c.Id AS contactId, c.Value AS value
            FROM third_party_contact c
            JOIN contact_type t ON t.Id = c.ContactTypeId
            WHERE t.Code = :typeCode
              AND c.IsPrimary = 1 AND c.IsVerified = 1
              AND c.Visible = 1 AND c.Enabled = 1
            """, nativeQuery = true)
    List<PrimaryVerifiedContact> findPrimaryVerifiedByTypeCode(@Param("typeCode") String typeCode);

    /** Proyeccion de la consulta anterior. */
    interface PrimaryVerifiedContact {
        String getThirdPartyId();
        String getContactId();
        String getValue();
    }

    /** Cuantos recibirian por este medio. Alimenta el recuento previo al lanzamiento. */
    @Query(value = """
            SELECT COUNT(*)
            FROM third_party_contact c
            JOIN contact_type t ON t.Id = c.ContactTypeId
            WHERE t.Code = :typeCode
              AND c.IsPrimary = 1 AND c.IsVerified = 1
              AND c.Visible = 1 AND c.Enabled = 1
            """, nativeQuery = true)
    long countPrimaryVerifiedByTypeCode(@Param("typeCode") String typeCode);

    /**
     * Lo mismo pero acotado a un puñado de personas concretas.
     *
     * <p>Lo usa finance para avisar de una liquidacion o de la nomina: ahi no
     * interesa toda la audiencia del sistema, solo los empleados de esa corrida.
     * Traer la lista completa y filtrar en memoria seria pedir la agenda entera
     * para llamar a tres personas.</p>
     */
    @Query(value = """
            SELECT c.ThirdPartyId AS thirdPartyId, c.Id AS contactId, c.Value AS value
            FROM third_party_contact c
            JOIN contact_type t ON t.Id = c.ContactTypeId
            WHERE t.Code = :typeCode
              AND c.IsPrimary = 1 AND c.IsVerified = 1
              AND c.Visible = 1 AND c.Enabled = 1
              AND c.ThirdPartyId IN (:thirdPartyIds)
            """, nativeQuery = true)
    List<PrimaryVerifiedContact> findPrimaryVerifiedByTypeCodeAndThirdParties(
            @Param("typeCode") String typeCode,
            @Param("thirdPartyIds") List<String> thirdPartyIds);

    /** Codigo del tipo de un contacto concreto. Lo usa la verificacion por OTP. */
    @Query(value = """
            SELECT t.Code
            FROM third_party_contact c
            JOIN contact_type t ON t.Id = c.ContactTypeId
            WHERE c.Id = :contactId
            """, nativeQuery = true)
    String findTypeCodeByContactId(@Param("contactId") String contactId);
}
