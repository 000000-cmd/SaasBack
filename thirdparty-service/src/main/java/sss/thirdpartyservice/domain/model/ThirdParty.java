package sss.thirdpartyservice.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter

public class ThirdParty extends BaseDomain  {

    private ThirdPartyType type;

    private UUID documentTypeId;

    private String documentNumber;

    private UUID userID;

    private String firstName;

    private String secondName;

    private String firstLastName;

    private String secondLastName;

    private String email;

    private String phone;

    private String businessName;

    private String tradeName;

    private String code;

    private boolean active;


    public ThirdParty() {
    }

    public ThirdParty(ThirdPartyType type, UUID documentTypeId, String documentNumber, UUID userID, String firstName, String secondName, String firstLastName, String secondLastName, String email, String phone, String businessName, String tradeName, boolean active) {
        this.type = type;
        this.documentTypeId = documentTypeId;
        this.documentNumber = documentNumber;
        this.userID = userID;
        this.firstName = firstName;
        this.secondName = secondName;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.email = email;
        this.phone = phone;
        this.businessName = businessName;
        this.tradeName = tradeName;
        this.active = active;
    }
}