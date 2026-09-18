package com.saas.thirdparty.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.thirdparty.application.dto.request.BankAccountRequest;
import com.saas.thirdparty.application.dto.response.BankAccountResponse;
import com.saas.thirdparty.domain.model.AccountKind;
import com.saas.thirdparty.domain.model.BankAccount;
import com.saas.thirdparty.infrastructure.persistence.repository.JpaBankAccountRepository.AccountView;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(config = BaseMapStructConfig.class)
public interface BankAccountMapper {

    BankAccount toDomain(BankAccountRequest req);

    void updateDomain(BankAccountRequest req, @MappingTarget BankAccount target);

    /**
     * Respuesta a partir de la PROYECCION, que ya trae el nombre del banco.
     *
     * <p>Se escribe a mano en vez de dejarlo a MapStruct porque la proyeccion
     * nativa devuelve todo como texto y el {@code label} lo compone el dominio:
     * ese texto es el que acaba en el comprobante y en el correo, y tiene que
     * salir de un solo sitio.</p>
     */
    default BankAccountResponse fromView(AccountView v) {
        BankAccount domain = BankAccount.builder()
                .accountKind(AccountKind.valueOf(v.getAccountKind()))
                .accountType(v.getAccountType())
                .accountNumber(v.getAccountNumber())
                .brevKey(v.getBrevKey())
                .build();

        return new BankAccountResponse(
                UUID.fromString(v.getId()),
                UUID.fromString(v.getThirdPartyId()),
                domain.getAccountKind(),
                v.getBankId() == null ? null : UUID.fromString(v.getBankId()),
                v.getBankName(),
                v.getAccountType(),
                v.getAccountNumber(),
                v.getBrevKey(),
                v.getAlias(),
                Boolean.TRUE.equals(v.getIsPrimary()),
                domain.label(v.getBankName()));
    }

    default List<BankAccountResponse> fromViews(List<AccountView> views) {
        return views.stream().map(this::fromView).toList();
    }
}
