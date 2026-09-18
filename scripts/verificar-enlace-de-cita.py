# -*- coding: utf-8 -*-
"""Comprueba que la cita de un cliente no se filtra por la URL.

Que se afirma:

  * los ultimos cuatro digitos del telefono YA NO viajan en el query string
    (la consulta es POST y el dato va en el cuerpo);
  * un enlace firmado abre la cita sin teclear nada;
  * y un enlace firmado NO sirve desde el subdominio de otro negocio, ni
    caducado, ni inventado — y los cuatro fallos responden EXACTAMENTE lo mismo,
    para no decirle a quien lo intenta cual de las dos partes acerto.

Uso:  python scripts/verificar-enlace-de-cita.py
"""
import base64
import hashlib
import hmac
import json
import subprocess
import sys
import time
from datetime import date, timedelta
import urllib.error
import urllib.request

GW = "http://localhost:8080"
SLUG = "cc2085"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
NOTA = "prueba enlace de cita"

MANANA = (date.today() + timedelta(days=3)).isoformat()

fallos = []


def check(nombre, cond, detalle=""):
    print(("  OK     " if cond else "  FALLA  ") + nombre + ("  " + detalle if detalle else ""))
    if not cond:
        fallos.append(nombre + " " + detalle)


def env(clave):
    for line in open(r"C:\SaasBack\.env", encoding="utf-8"):
        if line.startswith(clave + "="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("falta " + clave + " en .env")


def sql(q):
    p = subprocess.run(["docker", "exec", "-i", "saas-mysql", "mysql",
                        "--default-character-set=utf8mb4", "-uroot",
                        "-p" + env("MYSQL_ROOT_PASSWORD"), "-N", "-e", q],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if p.returncode != 0:
        raise SystemExit("SQL: " + p.stderr)
    return [l.split("\t") for l in p.stdout.splitlines() if "insecure" not in l]


def http(method, path, body=None):
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


def token(appointment_id, segundos):
    """El mismo algoritmo que AppointmentLinkService, para poder emitir uno."""
    secreto = env("BOOKING_LINK_SECRET").encode()
    cuerpo = ("%s.%d" % (appointment_id, int(time.time()) + segundos)).encode()
    datos = base64.urlsafe_b64encode(cuerpo).decode().rstrip("=")
    firma = hmac.new(secreto, datos.encode(), hashlib.sha256).digest()[:16]
    return datos + "." + base64.urlsafe_b64encode(firma).decode().rstrip("=")


def main():
    filas = sql("SELECT a.Id, a.PublicCode, RIGHT(c.PhoneE164,4) FROM saas_db.appointment a "
                "JOIN saas_db.business_client c ON c.Id = a.BusinessClientId "
                "WHERE a.BusinessId='%s' AND c.PhoneE164 IS NOT NULL LIMIT 1" % BIZ)
    if not filas:
        raise SystemExit("no hay citas sembradas: corre scripts/sembrar-demo-agenda.py")
    cita, codigo, last4 = filas[0]

    print("=== 1) el telefono no viaja en la URL ===")
    st, r = http("POST", "/business/public/booking/%s/appointments/%s/lookup" % (SLUG, codigo),
                 {"last4": last4})
    check("con el codigo y su telefono, la cita", st == 200 and r["data"]["publicCode"] == codigo,
          str(st))

    # El endpoint viejo mandaba ?last4=; si volviera, esto lo caza.
    st, _ = http("GET", "/business/public/booking/%s/appointments/%s?last4=%s"
                 % (SLUG, codigo, last4))
    check("el GET con ?last4= ya no existe", st in (404, 405), str(st))

    print("=== 2) el telefono equivocado no distingue de una cita que no existe ===")
    st1, r1 = http("POST", "/business/public/booking/%s/appointments/%s/lookup" % (SLUG, codigo),
                   {"last4": "0000"})
    st2, r2 = http("POST", "/business/public/booking/%s/appointments/NOEXISTE/lookup" % SLUG,
                   {"last4": last4})
    check("los dos responden igual", st1 == st2, "%s vs %s" % (st1, st2))

    print("=== 3) el enlace firmado abre la cita sin teclear nada ===")
    st, r = http("GET", "/business/public/booking/%s/appointments/by-token/%s"
                 % (SLUG, token(cita, 3600)))
    check("entra con el token", st == 200 and r["data"]["publicCode"] == codigo, str(st))

    print("=== 4) y no sirve de ninguna otra forma ===")
    otro = sql("SELECT Slug FROM saas_db.business_domain WHERE BusinessId <> '%s' "
               "AND Enabled = 1 LIMIT 1" % BIZ)
    respuestas = []

    if otro:
        # El mismo token, desde el subdominio de OTRO negocio: si esto pasara,
        # cualquier dueño podria leer los clientes del de al lado.
        st, r = http("GET", "/business/public/booking/%s/appointments/by-token/%s"
                     % (otro[0][0], token(cita, 3600)))
        check("no sirve desde otro negocio", st >= 400, str(st))
        respuestas.append(r.get("message"))

    st, r = http("GET", "/business/public/booking/%s/appointments/by-token/%s"
                 % (SLUG, token(cita, -10)))
    check("no sirve caducado", st >= 400, str(st))
    respuestas.append(r.get("message"))

    st, r = http("GET", "/business/public/booking/%s/appointments/by-token/aaaa.bbbb" % SLUG)
    check("no sirve inventado", st >= 400, str(st))
    respuestas.append(r.get("message"))

    # Firma cambiada: el cuerpo es bueno, la firma no.
    bueno = token(cita, 3600)
    roto = bueno[:-4] + ("aaaa" if not bueno.endswith("aaaa") else "bbbb")
    st, r = http("GET", "/business/public/booking/%s/appointments/by-token/%s" % (SLUG, roto))
    check("no sirve con la firma cambiada", st >= 400, str(st))
    respuestas.append(r.get("message"))

    check("y todos dicen lo MISMO", len(set(respuestas)) == 1,
          " | ".join(sorted(set(str(x)[:40] for x in respuestas))))

    print("=== 5) el aviso al cliente lleva ESE enlace, no el codigo ===")
    # Se reserva de verdad y se mira lo que salio al outbox: el aviso viaja por
    # correo, SMS y WhatsApp, y hasta ahora llevaba /mi-cita?codigo=XXXX — el
    # codigo de la cita escrito en la URL de un mensaje.
    st, r = http("GET", "/business/public/booking/%s/availability"
                        "?branchId=%s&offeringIds=%s&from=%s&to=%s"
                 % (SLUG, BRANCH, OFF, MANANA, MANANA))
    slots = r.get("data", {}).get("slots") or []
    if not slots:
        check("hay hueco para la prueba", False, "sin huecos")
    else:
        st, r = http("POST", "/business/public/booking/%s/appointments" % SLUG, {
            "branchId": BRANCH, "employeeId": EMP, "startUtc": slots[-1]["startUtc"],
            "offeringIds": [OFF], "clientName": "Prueba Enlace",
            "clientPhone": "3005551234", "whatsappOptIn": False, "notes": NOTA})
        check("la reserva se crea", st == 200, str(st))

        filas = sql("SELECT Payload FROM saas_db.outbox_event WHERE EventType LIKE '%%NOTIF%%' "
                    "ORDER BY CreatedAt DESC LIMIT 1")
        payload = filas[0][0] if filas else ""
        enlace = json.loads(payload).get("data", {}).get("LINK", "") if payload else ""
        check("el aviso lleva un enlace /c/", "/c/" in enlace, enlace[:60])
        check("y NO lleva el codigo en la URL", "codigo=" not in enlace, enlace[:60])

        if "/c/" in enlace:
            st, r = http("GET", "/business/public/booking/%s/appointments/by-token/%s"
                         % (SLUG, enlace.rsplit("/c/", 1)[1]))
            check("ese enlace abre la cita", st == 200, str(st))

        # Se limpia lo que creo esta comprobacion.
        for t in ("appointment_history", "appointment_service", "appointment_notification"):
            sql("DELETE FROM saas_db.%s WHERE AppointmentId IN "
                "(SELECT Id FROM saas_db.appointment WHERE Notes='%s')" % (t, NOTA))
        sql("DELETE FROM saas_db.appointment WHERE Notes='%s'" % NOTA)

    print()
    if fallos:
        print("FALLARON %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


main()
