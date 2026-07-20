package com.saas.search.domain.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read model del saldo por cobrar del empleado. Proyectado desde finance
 * (evento {@code finance.balance.updated}). El APK lo consulta por {@code userId}
 * o {@code employeeId} para pintar el saldo sin pegarle a la BD transaccional.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(
        indexName = "#{@indexNames.employeeBalances()}",
        createIndex = false,
        writeTypeHint = WriteTypeHint.FALSE
)
public class EmployeeBalanceDocument extends BaseDocument {

    @Field(type = FieldType.Keyword)
    private UUID branchId;

    @Field(type = FieldType.Keyword)
    private UUID employeeId;

    @Field(type = FieldType.Keyword)
    private UUID thirdPartyId;

    @Field(type = FieldType.Keyword)
    private UUID userId;

    @Field(type = FieldType.Double)
    private BigDecimal amountAccrued;

    @Field(type = FieldType.Double)
    private BigDecimal amountPaid;

    @Field(type = FieldType.Double)
    private BigDecimal balance;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Keyword)
    private String lastCalculatedAt;
}
