package com.saas.thirdparty.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.util.UUID;

/**
 * Cuenta a la que se le consigna la nomina a una persona.
 *
 * <p>Existe porque el dia de pago el dueño necesita el dato A LA MANO. Sin esto
 * tenia que perseguir a cada empleado por WhatsApp justo mientras esta pagando,
 * que es el peor momento posible.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BankAccount extends BaseDomain {

    private UUID thirdPartyId;
    private AccountKind accountKind;

    /** Solo BANK: FK al catalogo de bancos. */
    private UUID bankId;
    /** Solo BANK: SAVINGS | CHECKING. */
    private String accountType;
    /** Solo BANK. */
    private String accountNumber;
    /** Solo BREV, literal. */
    private String brevKey;

    /** Como la reconoce su dueño ("la de la nomina"). Opcional. */
    private String alias;
    private Boolean isPrimary;

    public static final String SAVINGS = "SAVINGS";
    public static final String CHECKING = "CHECKING";

    /**
     * Lo que se ve en el comprobante. Se compone aqui y no en la pantalla porque
     * el mismo texto lo pintan la web, el correo y el PDF, y tres versiones del
     * mismo dato acaban diciendo cosas distintas.
     *
     * <p>El numero va ENMASCARADO salvo los ultimos cuatro: un comprobante se
     * reenvia por correo y por WhatsApp, y no tiene por que llevar la cuenta
     * completa de nadie.</p>
     *
     * @param bankName nombre del banco ya resuelto; puede venir null.
     */
    public String label(String bankName) {
        if (accountKind == AccountKind.BREV) {
            return "Llave BREV · " + (brevKey == null ? "" : brevKey);
        }
        String tipo = CHECKING.equals(accountType) ? "Corriente" : "Ahorros";
        return String.join(" · ",
                bankName == null || bankName.isBlank() ? "Banco" : bankName,
                tipo,
                mask(accountNumber));
    }

    private static String mask(String number) {
        if (number == null || number.isBlank()) return "";
        String clean = number.trim();
        return clean.length() <= 4 ? clean : "···" + clean.substring(clean.length() - 4);
    }
}
