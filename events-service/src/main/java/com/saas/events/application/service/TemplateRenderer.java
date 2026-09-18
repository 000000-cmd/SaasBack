package com.saas.events.application.service;

import com.saas.events.domain.model.ChannelType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sustituye {{PARAMETRO}} por su valor. Clase pura: no toca BD ni red, de modo
 * que la previsualizacion y el envio real pasan por exactamente el mismo
 * codigo y no pueden divergir.
 *
 * Tres reglas que no son negociables:
 *
 * 1. En EMAIL los valores se escapan como HTML. El dato viene de fuera y
 *    termina en un documento que alguien abre: es una frontera de confianza.
 *    En SMS y WhatsApp no se escapa, porque no son HTML y &amp; se leeria literal.
 *
 * 2. Un parametro desconocido se deja LITERAL. Que se vea {{FOO}} es
 *    infinitamente mejor que un hueco silencioso que nadie detecta en meses.
 *
 * 3. Solo se reconocen codigos en MAYUSCULAS empezando por letra. Si se
 *    admitieran minusculas o espacios, la sustitucion se volveria ambigua.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([A-Z][A-Z0-9_]*)\\s*\\}\\}");

    public String render(String text, Map<String, String> data, ChannelType channel) {
        if (text == null || text.isEmpty()) return text;
        Map<String, String> values = data == null ? Map.of() : data;
        boolean escape = channel == ChannelType.EMAIL;

        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String code = m.group(1);
            String value = values.get(code);
            // Sin valor -> se deja el marcador tal cual (regla 2).
            String replacement = value == null
                    ? m.group(0)
                    : (escape ? escapeHtml(value) : value);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Codigos presentes en el texto que no traen valor. Alimenta el aviso de la bitacora. */
    public List<String> missingParameters(String text, Map<String, String> data) {
        if (text == null || text.isEmpty()) return List.of();
        Map<String, String> values = data == null ? Map.of() : data;
        Set<String> missing = new LinkedHashSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            if (!values.containsKey(m.group(1))) missing.add(m.group(1));
        }
        return new ArrayList<>(missing);
    }

    private static String escapeHtml(String raw) {
        return raw.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
