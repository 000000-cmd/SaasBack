# -*- coding: utf-8 -*-
"""Comprueba el webhook de WhatsApp y la conversacion que reserva.

Que prueba: que el apreton de manos de Meta solo pasa con el token bueno, que un
webhook sin firma o con firma falsa se RECHAZA, que una entrega buena se acepta
y se guarda cruda, que una reentrega no vuelve a contestar (Meta reentrega), que
la conversacion recorre los pasos del flujo configurado y acaba creando una cita
de verdad, que el paso de verificar el telefono se salta —el numero ya lo
verifico WhatsApp— y que con el canal apagado el bot no atiende.

No hace falta cuenta de Meta: se firman los payloads con el secreto de
desarrollo, que es exactamente lo que hace Meta.

Como usarlo: con el stack arriba, `python scripts/verificar-whatsapp.py`.
Limpia lo que crea, asi que se puede repetir.
"""
import hashlib
import hmac
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

GW = "http://localhost:8080"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
PHONE_ID = "prueba-wa-phone-id"
CLIENTE = "+573001234599"
NOTA = "Reservada por WhatsApp"

fallos = []


def check(nombre, cond, detalle=""):
    print(("  OK   " if cond else "  FALLA") + "  " + nombre + ("  " + detalle if detalle else ""))
    if not cond:
        fallos.append(nombre + " " + detalle)


def env(clave):
    for line in open(r"C:\SaasBack\.env", encoding="utf-8"):
        if line.startswith(clave + "="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("falta " + clave + " en .env")


def sql(q, db="saas_db"):
    p = subprocess.run(["docker", "exec", "-i", "saas-mysql", "mysql",
                        "--default-character-set=utf8mb4", "-uroot",
                        "-p" + env("MYSQL_ROOT_PASSWORD"), "-D", db, "-N", "-e", q],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if p.returncode != 0:
        raise SystemExit("SQL: " + p.stderr)
    return [l.split("\t") for l in p.stdout.splitlines() if "insecure" not in l]


def http(method, path, body=None, headers=None, raw=None):
    data = raw.encode() if raw is not None else (json.dumps(body).encode() if body else None)
    req = urllib.request.Request(GW + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def firmar(cuerpo):
    """Lo mismo que hace Meta: HMAC-SHA256 del cuerpo con el secreto de la app."""
    mac = hmac.new(env("WHATSAPP_APP_SECRET").encode(), cuerpo.encode(), hashlib.sha256)
    return "sha256=" + mac.hexdigest()


def entrega(texto, wa_id=None):
    """Un webhook de Meta con un mensaje de texto."""
    wa_id = wa_id or ("wamid.PRUEBA" + uuid.uuid4().hex[:12])
    cuerpo = json.dumps({
        "object": "whatsapp_business_account",
        "entry": [{"id": "0", "changes": [{"field": "messages", "value": {
            "messaging_product": "whatsapp",
            "metadata": {"display_phone_number": "573000000000",
                          "phone_number_id": PHONE_ID},
            "messages": [{"from": CLIENTE.lstrip("+"), "id": wa_id,
                           "timestamp": str(int(time.time())),
                           "type": "text", "text": {"body": texto}}],
        }}]}],
    })
    return wa_id, cuerpo


def cuantas_respuestas():
    filas = sql("SELECT COUNT(*) FROM outbox_event WHERE AggregateType='whatsapp_reply'")
    return int(filas[0][0]) if filas else 0


def hablar(texto, wa_id=None):
    """Manda un mensaje firmado y devuelve lo que el bot contesto.

    Se espera a que aparezca una respuesta NUEVA, no a que exista alguna:
    despues del primer mensaje siempre hay una, y leer "la ultima" devolvia la
    del turno anterior — la prueba parecia fallar cuando el bot iba bien.
    """
    antes = cuantas_respuestas()
    wa_id, cuerpo = entrega(texto, wa_id)
    st, _ = http("POST", "/business/public/whatsapp/webhook", raw=cuerpo,
                 headers={"X-Hub-Signature-256": firmar(cuerpo)})
    if st != 200:
        return st, wa_id, None
    hasta = time.time() + 20
    while time.time() < hasta and cuantas_respuestas() <= antes:
        time.sleep(0.5)
    return st, wa_id, ultimo_texto()


def ultimo_texto():
    """El ultimo mensaje que el bot compuso, leido del payload del outbox."""
    filas = sql("SELECT Payload FROM outbox_event WHERE AggregateType='whatsapp_reply' "
                "ORDER BY CreatedAt DESC LIMIT 1")
    if not filas:
        return None
    try:
        return json.loads(filas[0][0])["data"]["MENSAJE"]
    except Exception:
        return filas[0][0][:200]


def limpiar():
    sql("DELETE FROM whatsapp_session WHERE PhoneE164='%s'" % CLIENTE)
    sql("DELETE FROM whatsapp_message WHERE FromPhone='%s'" % CLIENTE)
    citas = "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA
    sql("DELETE FROM appointment_review WHERE AppointmentId IN " + citas)
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN " + citas)
    sql("DELETE FROM service_charge WHERE AppointmentId IN " + citas)
    for t in ("appointment_history", "appointment_service"):
        sql("DELETE FROM %s WHERE AppointmentId IN %s" % (t, citas))
    sql("DELETE FROM appointment WHERE Notes='%s'" % NOTA)
    sql("DELETE FROM business_client WHERE PhoneE164='%s'" % CLIENTE)
    sql("DELETE FROM outbox_event WHERE AggregateType='whatsapp_reply'")
    sql("DELETE FROM notification_log WHERE NotificationCode='WHATSAPP_REPLY'", db="saas_events")


def main():
    limpiar()
    # El negocio atiende por WhatsApp y este es su numero.
    sql("UPDATE business_booking_policy SET WhatsappEnabled = 1, WhatsappPhoneId = '%s' "
        "WHERE BusinessId='%s'" % (PHONE_ID, BIZ))

    print("=== 1) el apreton de manos solo pasa con el token bueno ===")
    st, cuerpo = http("GET", "/business/public/whatsapp/webhook"
                             "?hub.mode=subscribe&hub.verify_token=%s&hub.challenge=42"
                             % env("WHATSAPP_VERIFY_TOKEN"))
    check("con el token correcto devuelve el reto", st == 200 and cuerpo.strip() == "42",
          "%s %s" % (st, cuerpo[:30]))
    st, _ = http("GET", "/business/public/whatsapp/webhook"
                        "?hub.mode=subscribe&hub.verify_token=inventado&hub.challenge=42")
    check("con un token inventado responde 403", st == 403, str(st))

    print("=== 2) sin firma valida NO se acepta nada ===")
    _, cuerpo = entrega("hola")
    st, _ = http("POST", "/business/public/whatsapp/webhook", raw=cuerpo)
    check("sin cabecera de firma: 403", st == 403, str(st))

    st, _ = http("POST", "/business/public/whatsapp/webhook", raw=cuerpo,
                 headers={"X-Hub-Signature-256": "sha256=" + "0" * 64})
    check("con firma falsa: 403", st == 403, str(st))

    st, _ = http("POST", "/business/public/whatsapp/webhook", raw=cuerpo + " ",
                 headers={"X-Hub-Signature-256": firmar(cuerpo)})
    check("firma buena pero cuerpo alterado: 403", st == 403, str(st))
    check("y nada de eso se guardo",
          not sql("SELECT Id FROM whatsapp_message WHERE FromPhone='%s'" % CLIENTE))

    print("=== 3) la conversacion arranca y ofrece servicios ===")
    st, wa1, r1 = hablar("hola")
    check("entrega aceptada", st == 200, str(st))
    check("el mensaje quedo guardado crudo",
          bool(sql("SELECT RawPayload FROM whatsapp_message WHERE WaMessageId='%s'" % wa1)))
    check("el bot ofrece una lista numerada", r1 is not None and "1." in (r1 or ""),
          (r1 or "")[:70].replace("\n", " | "))

    print("=== 4) una reentrega NO vuelve a contestar ===")
    antes = len(sql("SELECT Id FROM outbox_event WHERE AggregateType='whatsapp_reply'"))
    _, cuerpo = entrega("hola", wa1)
    st, _ = http("POST", "/business/public/whatsapp/webhook", raw=cuerpo,
                 headers={"X-Hub-Signature-256": firmar(cuerpo)})
    check("la reentrega responde 200 igual", st == 200, str(st))
    time.sleep(2)
    despues = len(sql("SELECT Id FROM outbox_event WHERE AggregateType='whatsapp_reply'"))
    check("y no salio un segundo mensaje", antes == despues, "%d -> %d" % (antes, despues))

    print("=== 5) recorre los pasos hasta reservar ===")
    st, _, r2 = hablar("1")          # servicio
    check("tras el servicio pregunta el profesional",
          r2 is not None and ("quién" in (r2 or "") or "Cualquiera" in (r2 or "")),
          (r2 or "")[:70].replace("\n", " | "))

    st, _, r3 = hablar("1")          # cualquiera
    check("y despues ofrece horas",
          r3 is not None and ":" in (r3 or ""), (r3 or "")[:70].replace("\n", " | "))

    st, _, r4 = hablar("1")          # primera hora
    check("y despues pide el nombre",
          r4 is not None and "nombre" in (r4 or "").lower(),
          (r4 or "")[:70].replace("\n", " | "))

    st, _, r5 = hablar("Camilo Pérez")
    check("y muestra el resumen antes de confirmar",
          r5 is not None and "Resumen" in (r5 or ""), (r5 or "")[:70].replace("\n", " | "))
    # Aqui esta la prueba de que VERIFI se salta: nunca pidio un codigo.
    check("nunca pidio un codigo de verificacion",
          all("código" not in (x or "").lower() or "Tu código" in (x or "")
              for x in [r1, r2, r3, r4, r5]))

    st, _, r6 = hablar("1")          # confirmar
    check("confirma con el codigo publico de la cita",
          r6 is not None and "código" in (r6 or "").lower(),
          (r6 or "")[:80].replace("\n", " | "))

    citas = sql("SELECT Id, Status, Channel, PublicCode FROM appointment WHERE Notes='%s'" % NOTA)
    check("la cita existe de verdad", len(citas) == 1, "%d citas" % len(citas))
    if citas:
        check("y entro por el canal WHATSAPP", citas[0][2] == "WHATSAPP", citas[0][2])
        check("con su codigo publico", len(citas[0][3]) >= 6, citas[0][3])

    print("=== 6) el cliente queda con el telefono ya verificado ===")
    cli = sql("SELECT PhoneE164, PhoneVerifiedAt FROM business_client WHERE PhoneE164='%s'" % CLIENTE)
    check("se creo el cliente con su numero", len(cli) == 1, str(len(cli)))
    if cli:
        # Lo verifico WhatsApp al entregar el mensaje desde ese numero.
        check("y marcado como verificado", cli[0][1] not in ("NULL", "", None), str(cli[0][1]))

    print("=== 7) la conversacion se cierra al reservar ===")
    check("no queda sesion abierta",
          not sql("SELECT Id FROM whatsapp_session WHERE PhoneE164='%s'" % CLIENTE))

    print("=== 8) la respuesta sale del numero DE ESE NEGOCIO ===")
    # El evento tiene que llevar el negocio: es lo que hace que el envio use
    # las credenciales del negocio y no las de la plataforma. Sin esto, el bot
    # contestaria desde un numero que el cliente no reconoce — y al que no
    # puede responder.
    # BusinessId es BINARY(16) en el outbox, no texto: leerlo en crudo devuelve
    # bytes ilegibles. Se compara por su hexadecimal.
    esperado = BIZ.replace("-", "").upper()
    duenos = sql("SELECT DISTINCT HEX(BusinessId) FROM outbox_event "
                 "WHERE AggregateType='whatsapp_reply'")
    check("todas las respuestas van marcadas con el negocio",
          duenos and all(d[0] == esperado for d in duenos),
          str([d[0] for d in duenos])[:70])

    print("=== 9) con el canal apagado el bot no atiende ===")
    sql("UPDATE business_booking_policy SET WhatsappEnabled = 0 WHERE BusinessId='%s'" % BIZ)
    st, _, r7 = hablar("hola")
    check("responde 200 igual", st == 200, str(st))
    check("pero dice que no atiende por aqui",
          r7 is not None and "1." not in (r7 or ""), (r7 or "")[:70].replace("\n", " | "))

    return terminar()


def terminar():
    limpiar()
    sql("UPDATE business_booking_policy SET WhatsappEnabled = 0, WhatsappPhoneId = NULL "
        "WHERE BusinessId='%s'" % BIZ)
    print("")
    if fallos:
        print("FALLAN %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


if __name__ == "__main__":
    main()
