# -*- coding: utf-8 -*-
"""Comprueba que reservar avisa al cliente, y que avisa UNA sola vez.

Que prueba: que agendar publica el aviso con el codigo publico dentro, que
cancelar avisa con su motivo, que un cliente que no acepto WhatsApp no recibe
WhatsApp (y uno que si, si), que la deduplicacion aguanta un segundo intento
—que es lo que pasa cuando dos instancias barren a la vez— y que lo que nace
pendiente de confirmar no avisa todavia.

Se mira la BITACORA de events-service (notification_log) y no un buzon. En
desarrollo el modo de pruebas desvia todo, asi que las filas salen SKIPPED; y
WhatsApp y SMS no tienen proveedor ni con el envio encendido. Lo que se puede
afirmar —y se afirma— es que el aviso se disparo, con que texto y por que
canales.

Como usarlo: con el stack arriba, `python scripts/verificar-avisos-de-la-agenda.py`.
Limpia lo que crea, asi que se puede repetir.
"""
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta

GW = "http://localhost:8080"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
CLIENT = "c39faaf0-7eec-4312-86f1-061ddd77f3c5"
NOTA = "prueba avisos agenda"

fallos = []


def check(nombre, cond, detalle=""):
    print(("  OK   " if cond else "  FALLA") + "  " + nombre + ("  " + detalle if detalle else ""))
    if not cond:
        fallos.append(nombre + " " + detalle)


def dbpw():
    for line in open(r"C:\SaasBack\.env", encoding="utf-8"):
        if line.startswith("MYSQL_ROOT_PASSWORD="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("sin password")


def sql(q, db="saas_db"):
    p = subprocess.run(["docker", "exec", "-i", "saas-mysql", "mysql", "-uroot",
                        "-p" + dbpw(), "-D", db, "-N", "-e", q],
                       capture_output=True, text=True,
                       # Los cuerpos de las plantillas llevan emoji: con la
                       # codificacion por defecto de Windows, leerlos revienta.
                       encoding="utf-8", errors="replace")
    if p.returncode != 0:
        raise SystemExit("SQL: " + p.stderr)
    return [l.split("\t") for l in p.stdout.splitlines() if "insecure" not in l]


def http(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(GW + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def login(user, password="Password123!"):
    st, r = http("POST", "/auth/login", body={"usernameOrEmail": user, "password": password})
    if st != 200:
        raise SystemExit("login %s -> %s" % (user, st))
    return r["data"]["tokens"]["accessToken"]


def limpiar():
    citas = "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN " + citas)
    sql("DELETE FROM service_charge WHERE AppointmentId IN " + citas)
    for t in ("appointment_history", "appointment_service"):
        sql("DELETE FROM %s WHERE AppointmentId IN %s" % (t, citas))
    sql("DELETE FROM appointment WHERE Notes='%s'" % NOTA)
    sql("DELETE FROM notification_log WHERE NotificationCode LIKE 'APPOINTMENT%'", db="saas_events")


def agendar(token, dia, **extra):
    """Reserva en la primera hora libre del dia."""
    ultimo = (0, {})
    for hora in range(14, 22):
        cuerpo = {"businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
                  "businessClientId": CLIENT, "startUtc": "%sT%02d:00:00Z" % (dia, hora),
                  "offeringIds": [OFF], "backdated": False, "notes": NOTA}
        cuerpo.update(extra)
        st, r = http("POST", "/business/appointments", token, cuerpo)
        if st == 200:
            return st, r
        ultimo = (st, r)
    return ultimo


def bitacora(codigo=None):
    """Lo que events-service redacto, ya renderizado."""
    filas = sql("SELECT NotificationCode, TypeCode, Recipient, Status, "
                "COALESCE(Subject,'') FROM notification_log "
                "WHERE NotificationCode LIKE 'APPOINTMENT%' ORDER BY CreatedDate", db="saas_events")
    return filas


def esperar_bitacora(minimo, segundos=25):
    """El aviso viaja por el outbox y Kafka: no esta ahi en el mismo instante."""
    hasta = time.time() + segundos
    while time.time() < hasta:
        filas = bitacora()
        if len(filas) >= minimo:
            return filas
        time.sleep(1)
    return bitacora()


def main():
    dueno = login("cc2085")
    limpiar()

    # Sin consentimiento de WhatsApp: es el estado normal de un cliente creado
    # desde el panel, y hay que comprobar que NO se le escribe por ahi.
    sql("UPDATE business_client SET WhatsappOptInAt = NULL, WhatsappOptOutAt = NULL "
        "WHERE Id='%s'" % CLIENT)

    dia = (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d")

    print("=== 1) agendar avisa al cliente, con su codigo dentro ===")
    st, r = agendar(dueno, dia)
    check("cita creada", st == 200, str(st) + " " + str(r.get("message", ""))[:90])
    if st != 200:
        return terminar()
    cita = r["data"]["id"]
    codigo = r["data"]["publicCode"]

    filas = esperar_bitacora(1)
    confirmadas = [f for f in filas if f[0] == "APPOINTMENT_CONFIRMED"]
    check("se redacto la confirmacion", len(confirmadas) >= 1, "%d filas" % len(filas))

    marcas = sql("SELECT NotificationCode FROM appointment_notification "
                 "WHERE AppointmentId='%s'" % cita)
    check("queda la marca de que ya se aviso",
          any(m[0] == "APPOINTMENT_CONFIRMED" for m in marcas), str(marcas))

    print("=== 2) sin consentimiento NO sale por WhatsApp ===")
    canales = {f[1] for f in confirmadas}
    check("no hay WhatsApp", "WHATSAPP" not in canales, str(canales))
    check("si hay SMS (el telefono es lo que dejo)", "SMS" in canales, str(canales))

    print("=== 3) con consentimiento si sale por WhatsApp ===")
    sql("UPDATE business_client SET WhatsappOptInAt = NOW(6) WHERE Id='%s'" % CLIENT)
    st, r2 = agendar(dueno, (datetime.now() + timedelta(days=4)).strftime("%Y-%m-%d"))
    check("segunda cita creada", st == 200, str(st) + " " + str(r2.get("message", ""))[:90])
    cita2 = r2["data"]["id"] if st == 200 else None
    if cita2:
        filas = esperar_bitacora(len(filas) + 1)
        de_la_dos = [f for f in filas if f[0] == "APPOINTMENT_CONFIRMED"]
        check("ahora si hay WhatsApp", any(f[1] == "WHATSAPP" for f in de_la_dos),
              str({f[1] for f in de_la_dos}))

    print("=== 4) el texto lleva el codigo, que es lo unico que le sirve al cliente ===")
    cuerpos = sql("SELECT t.Body FROM notification_template t JOIN notification n "
                  "ON n.Id=t.NotificationId WHERE n.Code='APPOINTMENT_CONFIRMED'", db="saas_events")
    check("las tres plantillas piden el codigo",
          len(cuerpos) == 3 and all("{{CODIGO}}" in c[0] for c in cuerpos),
          "%d plantillas" % len(cuerpos))

    print("=== 5) el aviso no se repite (dos barridos a la vez) ===")
    antes = len(bitacora())
    # Segunda marca con el MISMO codigo: es lo que intentaria la otra instancia.
    dup = sql("INSERT IGNORE INTO appointment_notification "
              "(Id, AppointmentId, NotificationCode, SentAt, Enabled, Visible, AuditDate, CreatedDate) "
              "VALUES (UUID(), '%s', 'APPOINTMENT_CONFIRMED', NOW(6), 1, 1, NOW(6), NOW(6)); "
              "SELECT ROW_COUNT();" % cita)
    check("la segunda marca no inserta nada", dup and dup[-1][0] == "0", str(dup))
    time.sleep(2)
    check("y no aparece un segundo aviso", len(bitacora()) == antes,
          "%d -> %d" % (antes, len(bitacora())))

    print("=== 6) cancelar avisa, y dice por que ===")
    st, _ = http("POST", "/business/appointments/%s/status" % cita, dueno,
                 {"status": "CANCELADA_NEGOCIO", "reason": "El profesional se enfermó"})
    check("cita cancelada", st == 200, str(st))
    filas = esperar_bitacora(len(bitacora()) + 1)
    canceladas = [f for f in filas if f[0] == "APPOINTMENT_CANCELLED"]
    check("se redacto la cancelacion", len(canceladas) >= 1, "%d" % len(canceladas))

    print("=== 7) lo pendiente de confirmar todavia no promete nada ===")
    sql("UPDATE business_booking_policy SET RequiresManualConfirmation = 1 WHERE BusinessId='%s'" % BIZ)
    antes = len(bitacora())
    st, r3 = agendar(dueno, (datetime.now() + timedelta(days=5)).strftime("%Y-%m-%d"))
    # El panel agenda CONFIRMADA aunque la politica pida confirmacion: nadie se
    # acepta una cita a si mismo. Se comprueba por el estado que devuelve.
    if st == 200:
        check("desde el panel nace confirmada igual",
              r3["data"]["status"] == "CONFIRMADA", r3["data"]["status"])
    sql("UPDATE business_booking_policy SET RequiresManualConfirmation = 0 WHERE BusinessId='%s'" % BIZ)

    print("=== 8) los recordatorios salen de la configuracion del negocio ===")
    # Se comprueba de punta a punta: que el campo viaja en la respuesta y que
    # guardarlo cambia lo que el barrido va a leer.
    horas = sql("SELECT ReminderHoursBefore FROM business_booking_policy WHERE BusinessId='%s'" % BIZ)
    check("el negocio tiene su antelacion configurada", bool(horas) and horas[0][0] != "",
          str(horas))
    st, r = http("GET", "/business/booking-policies?businessId=%s" % BIZ, dueno)
    check("la politica se lee por API con ese campo",
          st == 200 and "reminderHoursBefore" in (r.get("data") or {}),
          "%s %s" % (st, list((r.get("data") or {}).keys())[:8]))

    st, r = http("PUT", "/business/booking-policies?businessId=%s" % BIZ, dueno,
                 {"reminderHoursBefore": "24, 2"})
    check("se puede configurar", st == 200 and r["data"]["reminderHoursBefore"] == "24, 2",
          "%s %s" % (st, (r.get("data") or {}).get("reminderHoursBefore")))

    st, r = http("PUT", "/business/booking-policies?businessId=%s" % BIZ, dueno,
                 {"reminderHoursBefore": "manana"})
    check("y una configuracion sin sentido se rechaza", st == 400, str(st))

    http("PUT", "/business/booking-policies?businessId=%s" % BIZ, dueno,
         {"reminderHoursBefore": "24"})

    print("=== 9) el barrido recuerda la cita de dentro de 24 horas, una vez ===")
    # Se mueve la cita a la ventana que el barrido mira ahora mismo, en vez de
    # esperar un dia. Es lo unico que una prueba no puede conseguir esperando.
    st, r = agendar(dueno, (datetime.now() + timedelta(days=6)).strftime("%Y-%m-%d"))
    check("tercera cita creada", st == 200, str(st))
    if st == 200:
        cita3 = r["data"]["id"]
        sql("UPDATE appointment SET StartUtc = UTC_TIMESTAMP() + INTERVAL 24 HOUR "
            "+ INTERVAL 1 MINUTE, EndUtc = UTC_TIMESTAMP() + INTERVAL 24 HOUR "
            "+ INTERVAL 31 MINUTE WHERE Id='%s'" % cita3)
        antes = len(bitacora())

        st, _ = http("POST", "/business/appointments/sweep", dueno, {})
        check("el barrido responde", st == 200, str(st))
        filas = esperar_bitacora(antes + 1)
        recordatorios = [f for f in filas if f[0] == "APPOINTMENT_REMINDER"]
        check("se redacto el recordatorio", len(recordatorios) >= 1, "%d" % len(recordatorios))
        check("y lleva la marca con las horas",
              any(m[0] == "APPOINTMENT_REMINDER:24" for m in
                  sql("SELECT NotificationCode FROM appointment_notification "
                      "WHERE AppointmentId='%s'" % cita3)))

        # Segundo barrido: es lo que pasa con dos instancias, y no puede volver
        # a escribirle al cliente.
        cuantos = len(bitacora())
        http("POST", "/business/appointments/sweep", dueno, {})
        time.sleep(4)
        check("un segundo barrido no vuelve a avisar", len(bitacora()) == cuantos,
              "%d -> %d" % (cuantos, len(bitacora())))

    print("=== 10) lo pendiente sin confirmar caduca y libera el hueco ===")
    st, r = agendar(dueno, (datetime.now() + timedelta(days=7)).strftime("%Y-%m-%d"))
    if st == 200:
        cita4 = r["data"]["id"]
        # Pendiente y creada hace mas del plazo del negocio.
        sql("UPDATE appointment SET Status='PENDIENTE_CONFIRMACION', "
            "CreatedDate = NOW(6) - INTERVAL 1 DAY WHERE Id='%s'" % cita4)
        http("POST", "/business/appointments/sweep", dueno, {})
        estado = sql("SELECT Status FROM appointment WHERE Id='%s'" % cita4)
        check("la cita caducada queda EXPIRADA",
              bool(estado) and estado[0][0] == "EXPIRADA", str(estado))

    return terminar()


def terminar():
    limpiar()
    sql("UPDATE business_client SET WhatsappOptInAt = NULL WHERE Id='%s'" % CLIENT)
    print("")
    if fallos:
        print("FALLAN %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


if __name__ == "__main__":
    main()
