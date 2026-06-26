package sss.thirdpartyservice.application.dto;

import java.util.UUID;

public record ThirdPartyResponse(

        UUID id,

        String documentNumber,

        String fullName,

        String email,

        String phone
) {
}