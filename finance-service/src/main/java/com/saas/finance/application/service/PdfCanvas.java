package com.saas.finance.application.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * El lienzo de los PDF: paleta, margenes, cursor y los pocos componentes que
 * los dos papeles comparten.
 *
 * <p>Existe porque hay DOS documentos —el comprobante de nomina y la factura del
 * cliente— y son el mismo sistema de diseno: la misma tinta morada, los mismos
 * 48pt de margen, la misma tabla. Duplicar el dibujo garantizaba que al retocar
 * uno el otro quedara distinto.</p>
 *
 * <p>PDFBox no tiene layout: todo va por coordenadas. De ahi el cursor {@link #y}
 * que cada bloque va bajando.</p>
 */
final class PdfCanvas {

    static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;
    static final PDFont REGULAR = PDType1Font.HELVETICA;

    // Paleta del sistema "Elite Document". Va en RGB fijo y SOLO en los PDF: un
    // fichero que se imprime y se archiva no resuelve variables de tema. Es la
    // misma excepcion documentada que la de las plantillas de correo.
    static final PDColor PRIMARY = rgb(0.510f, 0.016f, 0.745f);      // #8204be
    static final PDColor PRIMARY_SOFT = rgb(0.961f, 0.851f, 1f);     // #f5d9ff
    static final PDColor PRIMARY_DEEP = rgb(0.439f, 0f, 0.651f);     // #7000a6
    static final PDColor INK = rgb(0.082f, 0.110f, 0.153f);          // #151c27
    static final PDColor MUTED = rgb(0.373f, 0.369f, 0.369f);        // #5f5e5e
    static final PDColor LINE = rgb(0.820f, 0.757f, 0.835f);         // #d1c1d5
    static final PDColor SOFT = rgb(0.941f, 0.953f, 1f);             // #f0f3ff
    static final PDColor TINT = rgb(0.973f, 0.949f, 1f);             // #f8f2ff
    static final PDColor ZEBRA = rgb(0.984f, 0.961f, 0.996f);
    static final PDColor CHIP = rgb(0.906f, 0.933f, 0.996f);         // #e7eefe
    static final PDColor WHITE = rgb(1f, 1f, 1f);

    static final float LEFT = 48f;
    static final float RIGHT = PDRectangle.A4.getWidth() - 48f;
    static final float TOP = 790f;
    static final float BOTTOM = 70f;

    private final PDDocument doc;
    private PDPageContentStream cs;
    float y;

    PdfCanvas(PDDocument doc) throws IOException {
        this.doc = doc;
        newPage();
    }

    void newPage() throws IOException {
        if (cs != null) cs.close();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = TOP;
    }

    /** Salta de pagina si no queda sitio. El detalle puede ser largo. */
    void pageBreakIfNeeded(float needed) throws IOException {
        if (y - needed < BOTTOM) newPage();
    }

    void close() throws IOException { if (cs != null) cs.close(); }

    // =================================================================
    // Primitivas
    // =================================================================

    void text(PDFont font, float size, float x, float yy, String s, PDColor color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, yy);
        cs.showText(safe(s));
        cs.endText();
    }

    /**
     * Texto con separacion entre letras: es lo que da el aire del titulo y de
     * las versalitas del diseno. Se restaura a 0 al salir — el ajuste es estado
     * del stream y si no, se lleva por delante todo lo que venga despues.
     */
    void tracked(PDFont font, float size, float x, float yy, String s,
                 PDColor color, float spacing) throws IOException {
        cs.setCharacterSpacing(spacing);
        text(font, size, x, yy, s, color);
        cs.setCharacterSpacing(0);
    }

    void textRight(PDFont font, float size, float x, float yy, String s, PDColor color) throws IOException {
        text(font, size, x - width(font, size, s), yy, s, color);
    }

    void textCenter(PDFont font, float size, float cx, float yy, String s, PDColor color) throws IOException {
        text(font, size, cx - width(font, size, s) / 2, yy, s, color);
    }

    static float width(PDFont font, float size, String s) {
        try {
            return font.getStringWidth(safe(s)) / 1000 * size;
        } catch (IOException ex) {
            return 0f;
        }
    }

    void rect(float x, float yy, float w, float h, PDColor color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, yy, w, h);
        cs.fill();
    }

    void stroke(float x, float yy, float w, float h, PDColor color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(0.7f);
        cs.addRect(x, yy, w, h);
        cs.stroke();
    }

    void line(float x1, float yy, float x2, PDColor color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(0.7f);
        cs.moveTo(x1, yy);
        cs.lineTo(x2, yy);
        cs.stroke();
    }

    // =================================================================
    // Componentes
    // =================================================================

    /** Pastilla de metadato. Devuelve su ancho, para poder poner la siguiente al lado. */
    float chip(float x, float yy, String label, PDColor bg, PDColor border, PDColor fg) throws IOException {
        float w = width(BOLD, 7.5f, label) + label.length() * 0.6f + 22;
        rect(x, yy - 6, w, 18, bg);
        stroke(x, yy - 6, w, 18, border);
        tracked(BOLD, 7.5f, x + 11, yy, label, fg, 0.6f);
        return w;
    }

    /** Pastilla de estado, redonda y a contraste: lo primero que se busca en una factura. */
    void badge(float rightEdge, float yy, String label, PDColor bg, PDColor fg) throws IOException {
        float w = width(BOLD, 8f, label) + label.length() * 0.8f + 26;
        rect(rightEdge - w, yy - 6, w, 19, bg);
        tracked(BOLD, 8f, rightEdge - w + 13, yy, label, fg, 0.8f);
    }

    /** Etiqueta en versalitas moradas + su valor debajo. */
    void field(float x, float yy, String label, String value) throws IOException {
        tracked(BOLD, 7.5f, x, yy, up(label), PRIMARY, 0.9f);
        text(REGULAR, 10f, x, yy - 14, value == null || value.isBlank() ? "-" : value, INK);
    }

    /** Cabecera de tabla: banda clara y regla morada de 2pt debajo. */
    void tableHead(String c1, String c2, String c3, float colMid, float colRight) throws IOException {
        rect(LEFT, y - 8, RIGHT - LEFT, 24, PRIMARY_SOFT);
        tracked(BOLD, 7.5f, LEFT + 12, y, c1, INK, 0.9f);
        if (c2 != null) textRight(BOLD, 7.5f, colMid, y, c2, INK);
        textRight(BOLD, 7.5f, colRight, y, c3, INK);
        y -= 10;
        rect(LEFT, y, RIGHT - LEFT, 1.6f, PRIMARY);
        y -= 22;
    }

    /** Fila de concepto. {@code highlight} la tinta de fondo, como el padre de un detalle. */
    void row(String label, String mid, String amount, float colMid, float colRight,
             boolean highlight) throws IOException {
        if (highlight) rect(LEFT, y - 8, RIGHT - LEFT, 24, SOFT);
        text(BOLD, 10f, LEFT + 12, y, label, INK);
        if (mid != null) textRight(REGULAR, 8.5f, colMid, y, mid, MUTED);
        textRight(BOLD, 10f, colRight, y, amount, INK);
        y -= 16;
        line(LEFT, y, RIGHT, LINE);
        y -= 16;
    }

    /**
     * Sub-item: un servicio concreto bajo su concepto. Indentado, con la fecha en
     * su propia columna y zebra suave — se tiene que leer como detalle de la fila
     * de arriba, no como otro cargo.
     */
    void subRow(String date, String label, String amount, float colMid, float colRight,
                boolean zebra) throws IOException {
        if (zebra) rect(LEFT + 24, y - 6, RIGHT - LEFT - 24, 18, ZEBRA);
        text(REGULAR, 8.5f, LEFT + 34, y, date, MUTED);
        // El guillemot es Latin-1, asi que lo escriben las fuentes estandar del
        // PDF; la flecha del diseno (U+21B3) no, y saldria como un interrogante.
        text(REGULAR, 8.5f, LEFT + 92, y, "» " + label, INK);
        textRight(REGULAR, 8.5f, colMid, y, "1", MUTED);
        textRight(REGULAR, 8.5f, colRight, y, amount, INK);
        // 16pt y no 18: con 15 servicios —una quincena normal— es la diferencia
        // entre que el papel quepa en una hoja o se vaya a dos.
        y -= 16;
    }

    /**
     * Bloque legal sobre fondo claro, ANCLADO AL PIE de la pagina.
     *
     * <p>No se dibuja donde quedo el cursor sino abajo del todo: en el diseno el
     * pie esta pegado al borde inferior, y dejarlo flotando justo debajo de la
     * tabla convertia media pagina en un hueco. Si no cabe, salta de pagina y se
     * ancla igual — nunca queda una hoja con solo el pie.</p>
     */
    void termsBlock(String title, String[] lineas) throws IOException {
        float h = 30f + lineas.length * 12f;
        float anchor = BOTTOM + h + 18;
        // Cabe debajo de lo ya dibujado -> al pie. No cabe -> pagina nueva y
        // ARRIBA del todo, que deja una segunda hoja compacta en vez de una
        // hoja en blanco con dos lineas pegadas al borde inferior.
        if (anchor > y) newPage(); else y = anchor;
        rect(LEFT, y - h + 14, RIGHT - LEFT, h, SOFT);
        tracked(BOLD, 8f, LEFT + 14, y, title, PRIMARY_DEEP, 0.9f);
        float yy = y - 16;
        for (String l : lineas) {
            text(REGULAR, 7.5f, LEFT + 14, yy, "- " + l, MUTED);
            yy -= 12;
        }
        y = y - h - 6;
    }

    void footer(String note) throws IOException {
        line(LEFT, y + 6, RIGHT, LINE);
        tracked(BOLD, 7f, LEFT, y - 8, up(note), MUTED, 0.8f);
    }

    // =================================================================
    // Utilidades
    // =================================================================

    static PDColor rgb(float r, float g, float b) {
        return new PDColor(new float[] { r, g, b }, PDDeviceRGB.INSTANCE);
    }

    static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    static String cop(BigDecimal v) {
        return "$ " + String.format("%,.0f", nz(v)).replace(',', '.');
    }

    static String up(String s) { return safe(s).toUpperCase(); }

    /**
     * Las fuentes estandar de PDF solo saben escribir Latin-1: un caracter fuera
     * de ese rango revienta {@code showText} y se llevaria por delante todo el
     * documento. Se sustituye en vez de fallar.
     */
    static String safe(String s) {
        if (s == null || s.isBlank()) return "-";
        return s.replaceAll("[^\\x20-\\xFF]", "?");
    }
}
