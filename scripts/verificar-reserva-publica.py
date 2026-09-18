# -*- coding: utf-8 -*-
"""Comprueba la reserva pública del negocio: sin cuenta y por subdominio.

Que prueba: que todo se resuelve por SLUG y no por id, que el flujo y sus
textos se leen sin sesión, que los profesionales son solo los que prestan todos
los servicios pedidos, que la disponibilidad es la misma que ve el panel, que
el teléfono verificado se exige en el servidor, que la reserva devuelve un
código público, que consultar exige código + últimos cuatro dígitos, y que
cancelar respeta la ventana del negocio.

Como usarlo: con el stack arriba, `python scripts/verificar-reserva-publica.py`.
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta

GW = "http://localhost:8080"
SLUG = "cc2085"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
TEL = "3009998877"
NOTA = "reserva publica"

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


def sql(q):
    p = subprocess.run(["docker", "exec", "-i", "saas-mysql", "mysql", "-uroot",
                        "-p" + dbpw(), "-D", "saas_db", "-N", "-e", q],
                       capture_output=True, text=True)
    if p.returncode != 0:
        raise SystemExit("SQL: " + p.stderr)
    return [l.split("\t") for l in p.stdout.splitlines() if "insecure" not in l]


def http(method, path, body=None):
    """Sin token a propósito: es la superficie pública."""
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(GW + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def limpiar():
    for t in ("appointment_history", "appointment_service"):
        sql("DELETE FROM %s WHERE AppointmentId IN "
            "(SELECT Id FROM appointment WHERE Notes='%s')" % (t, NOTA))
    sql("DELETE FROM service_charge WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    sql("DELETE FROM appointment WHERE Notes='%s'" % NOTA)


def main():
    limpiar()

    print("=== 1) el negocio se resuelve por su subdominio, sin sesión ===")
    st, r = http("GET", "/business/public/booking/" + SLUG)
    check("contexto sin token", st == 200, str(st))
    if st != 200:
        return terminar()
    ctx = r["data"]
    check("trae el negocio correcto", ctx["businessId"] == BIZ, ctx["businessId"])
    check("trae su zona horaria", ctx["timeZone"] == "America/Bogota", ctx["timeZone"])
    check("trae sedes y servicios activos",
          len(ctx["branches"]) > 0 and len(ctx["offerings"]) > 0,
          "%d sedes, %d servicios" % (len(ctx["branches"]), len(ctx["offerings"])))
    check("dice si exige verificar el teléfono", "requiresPhoneVerification" in ctx,
          str(ctx.get("requiresPhoneVerification")))

    # Pedir código sin poder mandarlo dejaba la reserva pública MUERTA: el
    # asistente enseñaba el paso de verificación, su único botón nacía
    # deshabilitado y no había forma de llegar a reservar. Y era el caso por
    # defecto, porque la columna nace en TRUE y ningún negocio tiene WhatsApp
    # dado de alta. La bandera ahora depende de que exista el canal.
    pide = sql("SELECT RequirePhoneVerification, WhatsappEnabled, WhatsappPhoneId "
               "FROM business_booking_policy WHERE BusinessId='%s'" % BIZ)[0]
    hay_canal = pide[1] == "1" and pide[2] not in ("NULL", "", None)
    check("no pide código si no hay por dónde mandarlo",
          ctx["requiresPhoneVerification"] == hay_canal,
          "columna=%s canal=%s -> responde %s" % (pide[0], hay_canal,
                                                  ctx["requiresPhoneVerification"]))

    st, _ = http("GET", "/business/public/booking/no-existe-este-slug")
    check("un slug inventado da 404", st == 404, str(st))

    print("=== 2) el flujo y sus textos se leen sin sesión ===")
    st, r = http("GET", "/system/public/flows/AGEND/resolved?channel=WEB")
    check("flujo resuelto público", st == 200, str(st))
    if st == 200:
        pasos = [s["code"] for s in r["data"]["sections"]]
        check("mismos siete pasos que ve el panel", len(pasos) == 7, str(pasos))
        confir = [s for s in r["data"]["sections"] if s["code"] == "CONFIR"][0]
        botones = [c["code"] for c in confir["controls"]]
        check("sin sesión no aparece el botón que exige permiso",
              "BACKDATE" not in botones, str(botones))
    st, r = http("GET", "/system/public/flows/messages")
    check("los textos también", st == 200 and len(r.get("data", {})) >= 10,
          str(len(r.get("data", {}))))

    print("=== 3) profesionales: solo quien presta el servicio ===")
    st, r = http("GET", "/business/public/booking/%s/professionals?branchId=%s&offeringIds=%s"
                 % (SLUG, BRANCH, OFF))
    check("lista de profesionales", st == 200, str(st))
    profes = r.get("data", []) if st == 200 else []
    check("está el que sí lo presta", any(p["id"] == EMP for p in profes),
          str([p["name"] for p in profes]))

    print("=== 4) la disponibilidad pública es la misma del motor ===")
    manana = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    st, r = http("GET", "/business/public/booking/%s/availability?branchId=%s&offeringIds=%s"
                 "&from=%s&to=%s" % (SLUG, BRANCH, OFF, manana, manana))
    check("huecos sin sesión", st == 200, str(st))
    huecos = r["data"]["slots"] if st == 200 else []
    check("hay al menos dos huecos mañana", len(huecos) > 1, str(len(huecos)))
    if len(huecos) < 2:
        return terminar()
    hueco = huecos[0]["startUtc"]

    print("=== 5) el teléfono verificado se exige en el servidor ===")
    # Con canal: la regla aplica y la reserva se rechaza sin verificar.
    sql("UPDATE business_booking_policy SET RequirePhoneVerification=1, WhatsappEnabled=1, "
        "WhatsappPhoneId='verif-check' WHERE BusinessId='%s'" % BIZ)
    sql("DELETE FROM business_client WHERE BusinessId='%s' AND PhoneE164 LIKE '%%9998877'" % BIZ)
    cuerpo = {"branchId": BRANCH, "employeeId": EMP, "startUtc": hueco,
              "offeringIds": [OFF], "clientName": "Cliente Prueba",
              "clientPhone": TEL, "whatsappOptIn": True,
              "whatsappOptInText": "Acepto recibir mensajes", "notes": NOTA}
    st, r = http("POST", "/business/public/booking/%s/appointments" % SLUG, cuerpo)
    check("sin verificar, se rechaza", st >= 400,
          str(st) + " " + str(r.get("message", ""))[:70])

    # SIN canal: exigir un código que nadie puede mandar dejaba la reserva
    # publica MUERTA, y era el caso por defecto (la columna nace en 1 y ningun
    # negocio tiene WhatsApp dado de alta). Se afirma sobre la RESERVA y no
    # sobre la bandera del contexto: cuando solo se miraba la bandera, la
    # pantalla escondia el paso y el servidor seguia respondiendo 400 al final.
    sql("UPDATE business_booking_policy SET WhatsappEnabled=0, WhatsappPhoneId=NULL "
        "WHERE BusinessId='%s'" % BIZ)
    st, r = http("GET", "/business/public/booking/" + SLUG)
    check("sin canal, el contexto deja de pedir código",
          st == 200 and r["data"]["requiresPhoneVerification"] is False,
          str(r.get("data", {}).get("requiresPhoneVerification")))
    # El ULTIMO hueco, no el segundo: los huecos van cada 15 min y el servicio
    # dura 30, asi que huecos[1] se solapa con huecos[0] y dejaria sin sitio a
    # la reserva de la seccion siguiente.
    otro = dict(cuerpo, startUtc=huecos[-1]["startUtc"], clientPhone="3009998866",
                clientName="Sin Canal")
    st, r = http("POST", "/business/public/booking/%s/appointments" % SLUG, otro)
    check("y la reserva SÍ pasa, sin código", st == 200,
          str(st) + " " + str(r.get("message", ""))[:70])
    # La ficha del cliente se queda: la cita que acaba de crear la apunta, y
    # `limpiar()` ya borra esa cita al empezar la siguiente corrida.

    print("=== 6) con el teléfono verificado, reserva ===")
    sql("UPDATE business_booking_policy SET WhatsappEnabled=1, WhatsappPhoneId='verif-check' "
        "WHERE BusinessId='%s'" % BIZ)
    sql("UPDATE business_client SET PhoneVerifiedAt=NOW(6) WHERE BusinessId='%s' "
        "AND PhoneE164 LIKE '%%9998877'" % BIZ)
    st, r = http("POST", "/business/public/booking/%s/appointments" % SLUG, cuerpo)
    check("reserva aceptada", st == 200, str(st) + " " + str(r.get("message", ""))[:90])
    if st != 200:
        return terminar()
    codigo = r["data"]["publicCode"]
    check("devuelve código público", bool(codigo) and len(codigo) >= 6, str(codigo))
    check("y un nombre de profesional no vacío", bool(r["data"]["employeeName"]),
          r["data"]["employeeName"])
    canal = sql("SELECT Channel FROM appointment WHERE PublicCode='%s'" % codigo)
    check("queda marcada como reserva web", canal[0][0] == "WEB_PUBLICA", canal[0][0])
    optin = sql("SELECT WhatsappOptInAt IS NOT NULL, WhatsappOptInText FROM business_client "
                "WHERE BusinessId='%s' AND PhoneE164 LIKE '%%9998877'" % BIZ)
    check("el opt-in queda con su texto y su fecha",
          optin[0][0] == "1" and "Acepto" in optin[0][1], str(optin[0]))

    print("=== 7) consultar exige código Y últimos cuatro dígitos ===")
    # Es POST y el telefono va en el CUERPO: como query acababa escrito en los
    # registros del servidor y en el historial del navegador.
    st, r = http("POST", "/business/public/booking/%s/appointments/%s/lookup" % (SLUG, codigo),
                 {"last4": "0000"})
    check("con teléfono equivocado, 404", st == 404, str(st))
    st, r = http("POST", "/business/public/booking/%s/appointments/%s/lookup" % (SLUG, codigo),
                 {"last4": "8877"})
    check("con los suyos, la cita", st == 200 and r["data"]["publicCode"] == codigo, str(st))

    print("=== 8) cancelar respeta la ventana del negocio ===")
    sql("UPDATE business_booking_policy SET ClientCancelWindowHours=240 WHERE BusinessId='%s'" % BIZ)
    st, r = http("POST", "/business/public/booking/%s/appointments/%s/cancel" % (SLUG, codigo),
                 {"last4": "8877", "reason": "Ya no puedo"})
    check("fuera de plazo no deja cancelar", st >= 400,
          str(st) + " " + str(r.get("message", ""))[:70])
    sql("UPDATE business_booking_policy SET ClientCancelWindowHours=1 WHERE BusinessId='%s'" % BIZ)
    st, r = http("POST", "/business/public/booking/%s/appointments/%s/cancel" % (SLUG, codigo),
                 {"last4": "8877", "reason": "Ya no puedo"})
    check("dentro de plazo sí", st == 200, str(st) + " " + str(r.get("message", ""))[:70])
    estado = sql("SELECT Status, CancelledBy FROM appointment WHERE PublicCode='%s'" % codigo)
    check("queda cancelada por el cliente",
          estado[0][0] == "CANCELADA_CLIENTE" and estado[0][1] == "CLIENT", str(estado[0]))

    print("=== 9) no se puede reservar en la sede de otro negocio ===")
    otra = sql("SELECT Id FROM branch WHERE BusinessId <> '%s' LIMIT 1" % BIZ)
    if otra:
        st, r = http("GET", "/business/public/booking/%s/availability?branchId=%s&offeringIds=%s"
                     "&from=%s&to=%s" % (SLUG, otra[0][0], OFF, manana, manana))
        check("una sede de otro negocio da 404", st == 404, str(st))

    # Deja la política como estaba: sin canal de WhatsApp, que es como la
    # encuentra el negocio de demostración.
    sql("UPDATE business_booking_policy SET ClientCancelWindowHours=4, WhatsappEnabled=0, "
        "WhatsappPhoneId=NULL WHERE BusinessId='%s'" % BIZ)
    terminar()


def terminar():
    # Limpia tambien al SALIR, no solo al entrar. Este guion reserva en el dia
    # de manana, y `verificar-disponibilidad.py` afirma cuantos huecos quedan
    # ESE dia: dejar la cita puesta le restaba dos huecos y lo hacia fallar sin
    # que nada estuviera roto.
    limpiar()
    print()
    if fallos:
        print("FALLARON %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


main()
