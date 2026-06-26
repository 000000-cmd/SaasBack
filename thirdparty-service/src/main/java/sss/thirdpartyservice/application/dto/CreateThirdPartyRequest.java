package sss.thirdpartyservice.application.dto;




import sss.thirdpartyservice.domain.model.ThirdPartyType;

import java.util.UUID;

public record CreateThirdPartyRequest(

        ThirdPartyType type,

        UUID documentTypeId,

        String documentNumber,

        String firstName,

        String secondName,

        String firstLastName,

        String secondLastName,

        String email,

        String phone
) {
}
