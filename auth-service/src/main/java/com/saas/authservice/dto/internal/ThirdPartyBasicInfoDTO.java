package com.saas.authservice.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyBasicInfoDTO {
    private String firstName;
    private String firstLastName;
}