package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "business_booking_policy")
@SQLRestriction("Visible = 1")
public class BusinessBookingPolicyEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;

    @Column(name = "RequiresManualConfirmation", nullable = false) private Boolean requiresManualConfirmation = Boolean.FALSE;
    @Column(name = "ConfirmationTimeoutMinutes", nullable = false) private Integer confirmationTimeoutMinutes = 120;
    @Column(name = "MinLeadTimeMinutes", nullable = false) private Integer minLeadTimeMinutes = 30;
    @Column(name = "MaxHorizonDays", nullable = false) private Integer maxHorizonDays = 60;
    @Column(name = "SlotGranularityMinutes", nullable = false) private Integer slotGranularityMinutes = 15;
    @Column(name = "BufferBeforeMinutes", nullable = false) private Integer bufferBeforeMinutes = 0;
    @Column(name = "BufferAfterMinutes", nullable = false) private Integer bufferAfterMinutes = 0;
    @Column(name = "ClientCancelWindowHours", nullable = false) private Integer clientCancelWindowHours = 4;
    @Column(name = "RequirePhoneVerification", nullable = false) private Boolean requirePhoneVerification = Boolean.TRUE;

    @Column(name = "ReminderHoursBefore", length = 40, nullable = false) private String reminderHoursBefore = "24";

    @Column(name = "WhatsappEnabled", nullable = false) private Boolean whatsappEnabled = Boolean.FALSE;
    @Column(name = "WhatsappPhoneId", length = 40) private String whatsappPhoneId;
    @Column(name = "WhatsappWabaId", length = 40) private String whatsappWabaId;
    @Column(name = "WhatsappAccessToken", length = 2000) private String whatsappAccessToken;
    @Column(name = "WhatsappAppSecret", length = 2000) private String whatsappAppSecret;
    @Column(name = "WhatsappVerifyToken", length = 64) private String whatsappVerifyToken;
    @Column(name = "WhatsappWebhookAt") private LocalDateTime whatsappWebhookAt;
    @Column(name = "WhatsappDisplayPhone", length = 32) private String whatsappDisplayPhone;
    @Column(name = "WhatsappVerifiedName", length = 160) private String whatsappVerifiedName;
    @Column(name = "WhatsappVerifiedAt") private LocalDateTime whatsappVerifiedAt;
    @Column(name = "WhatsappMonthlyCount", nullable = false) private Integer whatsappMonthlyCount = 0;
    @Column(name = "WhatsappCountResetAt") private LocalDateTime whatsappCountResetAt;
}
