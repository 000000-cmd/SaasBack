package com.saas.search.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class BaseDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private UUID businessId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant updatedAt;

    /**
     * Version externa para idempotencia. Se setea con
     * {@code occurredAt.toEpochMilli()} del evento. Si llega un evento con
     * version menor, ES lo rechaza (no aplica updates stale).
     */
    @Field(type = FieldType.Long)
    private Long docVersion;

}
