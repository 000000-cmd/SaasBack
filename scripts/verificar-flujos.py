# -*- coding: utf-8 -*-
"""Comprueba el modulo de flujos contra el sistema levantado.

Que prueba: que el flujo resuelto trae solo lo del canal que se pide, que los
botones se filtran por permiso de verdad (no por rol), que lo del sistema no se
puede renombrar ni borrar pero si reconfigurar, que un flujo nuevo se crea y se
borra entero, y que el endpoint de registro retroactivo exige el mismo permiso
que esconde su boton.

Como usarlo: con el stack arriba, `python scripts/verificar-flujos.py`.
Los usuarios son los de las pruebas E2E del entorno de desarrollo.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

GW = "http://localhost:8080"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
CLIENT = "c39faaf0-7eec-4312-86f1-061ddd77f3c5"

# El administrador del sistema. Son las mismas credenciales que usa la suite
# E2E; se pueden cambiar por entorno sin tocar el script.
ADMIN_USER = os.environ.get("SAAS_ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("SAAS_ADMIN_PASS", "Admin123!")

fallos = []


def check(nombre, cond, detalle=""):
    print(("  OK   " if cond else "  FALLA") + "  " + nombre + ("  " + detalle if detalle else ""))
    if not cond:
        fallos.append(nombre + " " + detalle)


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
    st, r = http("POST", "/auth/login",
                 body={"usernameOrEmail": user, "password": password})
    if st != 200:
        raise SystemExit("login %s -> %s" % (user, st))
    return r["data"]["tokens"]["accessToken"]


def resolved(token, code, channel):
    st, r = http("GET", "/system/flows/%s/resolved?channel=%s" % (code, channel), token)
    if st != 200:
        raise SystemExit("resolved %s/%s -> %s %s" % (code, channel, st, json.dumps(r)[:200]))
    return r["data"]


def seccion(flujo, code):
    for s in flujo["sections"]:
        if s["code"] == code:
            return s
    return None


def main():
    dueno = login("cc2085")
    empleado = login("e2324")

    print("=== 1) el flujo del sistema sale resuelto y en orden ===")
    web = resolved(dueno, "AGEND", "WEB")
    codigos = [s["code"] for s in web["sections"]]
    check("siete pasos en web", len(codigos) == 7, str(codigos))
    check("en el orden configurado",
          codigos == ["SERVIC", "PROFES", "FECHOR", "DATBAS", "VERIFI", "CONFIR", "LISTO"],
          str(codigos))
    check("los pasos se numeran del 1 al 7",
          [s["step"] for s in web["sections"]] == list(range(1, 8)))

    print("=== 2) los campos traen su mascara del estandar ===")
    datbas = seccion(web, "DATBAS")
    campos = {c["code"]: c for c in datbas["fields"]}
    check("la seccion de datos es un formulario", datbas["kind"] == "FORM", datbas["kind"])
    check("el telefono lleva mascara de telefono",
          campos["phone"].get("mask") == "phone", str(campos["phone"].get("mask")))
    check("el nombre lleva mascara de nombre",
          campos["fullName"].get("mask") == "name", str(campos["fullName"].get("mask")))
    # El serializador omite los nulos, asi que "sin mascara" es que la clave
    # no venga. La pantalla lo lee igual: sin mascara, campo libre.
    check("las notas no llevan mascara (es texto libre)",
          campos["notes"].get("mask") is None, str(campos["notes"].get("mask")))
    check("el telefono es obligatorio y las notas no",
          campos["phone"]["required"] and not campos["notes"]["required"])

    print("=== 3) cada canal ve lo suyo ===")
    panel = resolved(dueno, "AGEND", "PANEL")
    check("el panel no pide verificar el telefono (VERIFI es de web y WhatsApp)",
          seccion(panel, "VERIFI") is None and len(panel["sections"]) == 6,
          str([s["code"] for s in panel["sections"]]))
    wa = resolved(dueno, "AGEND", "WHATSAPP")
    check("WhatsApp si la pide", seccion(wa, "VERIFI") is not None)
    botones_wa = [c["code"] for c in seccion(wa, "CONFIR")["controls"]]
    check("en WhatsApp solo queda confirmar (no hay boton de volver)",
          botones_wa == ["CONFIRM"], str(botones_wa))

    print("=== 4) los botones se filtran por PERMISO, no por rol ===")
    botones_dueno = [c["code"] for c in seccion(panel, "CONFIR")["controls"]]
    check("el dueño ve el registro retroactivo", "BACKDATE" in botones_dueno,
          str(botones_dueno))
    panel_emp = resolved(empleado, "AGEND", "PANEL")
    botones_emp = [c["code"] for c in seccion(panel_emp, "CONFIR")["controls"]]
    check("el empleado no lo ve", "BACKDATE" not in botones_emp, str(botones_emp))
    check("pero si ve confirmar", "CONFIRM" in botones_emp, str(botones_emp))

    print("=== 5) el endpoint exige el mismo permiso que esconde el boton ===")
    cuerpo = {"businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
              "businessClientId": CLIENT, "startUtc": "2026-09-01T14:00:00Z",
              "offeringIds": [OFF], "backdated": True, "notes": "prueba permiso"}
    st, r = http("POST", "/business/appointments", empleado, cuerpo)
    check("el empleado no puede registrar retroactivo", st == 403,
          str(st) + " " + str(r.get("message", ""))[:100])
    st, r = http("POST", "/business/appointments", dueno, cuerpo)
    check("el dueño si", st == 200, str(st) + " " + str(r.get("message", ""))[:100])

    print("=== 6) lo del sistema se reconfigura pero no se rompe ===")
    st, arbol = http("GET", "/system/flows/AGEND", dueno)
    check("la configuracion no la ve el dueño (es de administrador)", st == 403, str(st))

    admin = login(ADMIN_USER, ADMIN_PASS) if ADMIN_USER else None
    if admin:
        st, arbol = http("GET", "/system/flows/AGEND", admin)
        check("el administrador si", st == 200, str(st))
        flow_id = arbol["data"]["id"]
        st, r = http("PUT", "/system/flows/" + flow_id, admin,
                     {"code": "OTRO", "name": "Agendamiento"})
        check("renombrar el codigo del flujo del sistema se rechaza", st >= 400,
              str(st) + " " + str(r.get("message", ""))[:90])
        st, r = http("DELETE", "/system/flows/" + flow_id, admin)
        check("borrarlo tambien", st >= 400, str(st) + " " + str(r.get("message", ""))[:90])
        st, r = http("PUT", "/system/flows/" + flow_id, admin,
                     {"code": "AGEND", "name": "Agendamiento de citas"})
        check("cambiarle el nombre si se puede", st == 200,
              str(st) + " " + str(r.get("message", ""))[:90])

        print("=== 7) un flujo nuevo se crea, se resuelve y se borra ===")
        # Codigo distinto en cada pasada: borrar es logico, y reutilizar el
        # mismo codigo reactivaria el flujo de la pasada anterior (que es lo
        # que se comprueba aparte, mas abajo).
        codigo = "PRUEBA" + str(int(time.time()))[-5:]
        st, nuevo = http("POST", "/system/flows", admin,
                         {"code": codigo, "name": "Flujo de prueba"})
        check("flujo creado", st == 200, str(st) + " " + str(nuevo.get("message", ""))[:90])
        if st == 200:
            nid = nuevo["data"]["id"]
            st, con_seccion = http("POST", "/system/flows/%s/sections" % nid, admin,
                                   {"code": "UNO", "name": "Primer paso", "kind": "FORM",
                                    "channels": "WEB"})
            check("seccion creada", st == 200, str(st))
            sid = con_seccion["data"]["sections"][0]["id"]
            st, _ = http("POST", "/system/flows/sections/%s/fields" % sid, admin,
                         {"code": "email", "label": "Correo", "dataType": "email",
                          "maskCode": "email", "isRequired": True})
            check("campo creado", st == 200, str(st))
            st, _ = http("POST", "/system/flows/sections/%s/controls" % sid, admin,
                         {"code": "SAVE", "label": "Guardar", "action": "SAVE"})
            check("control creado", st == 200, str(st))

            prueba = resolved(admin, codigo, "WEB")
            check("el flujo nuevo se resuelve con su campo y su boton",
                  len(prueba["sections"]) == 1
                  and prueba["sections"][0]["fields"][0].get("mask") == "email"
                  and prueba["sections"][0]["controls"][0]["action"] == "SAVE")
            prueba_panel = resolved(admin, codigo, "PANEL")
            check("y en un canal que no declaro no aparece nada",
                  len(prueba_panel["sections"]) == 0)

            st, _ = http("DELETE", "/system/flows/" + nid, admin)
            check("flujo nuevo borrado", st == 200, str(st))
            st, _ = http("GET", "/system/flows/" + codigo, admin)
            check("y ya no se lista", st == 404, str(st))

            # Volver a crearlo con el mismo codigo lo RECUPERA entero. El
            # borrado es logico y la clave unica del codigo no mira Visible:
            # o se recupera, o el segundo intento choca contra el indice con
            # un error que no dice nada.
            st, revivido = http("POST", "/system/flows", admin,
                                {"code": codigo, "name": "Flujo de prueba"})
            check("crear otra vez con el mismo codigo lo reactiva", st == 200, str(st))
            if st == 200:
                check("y vuelve con su seccion, su campo y su boton",
                      len(revivido["data"]["sections"]) == 1
                      and len(revivido["data"]["sections"][0]["fields"]) == 1
                      and len(revivido["data"]["sections"][0]["controls"]) == 1)
                http("DELETE", "/system/flows/" + revivido["data"]["id"], admin)

    print("=== 8) los textos se piden por codigo ===")
    st, r = http("GET", "/system/flow-messages/map", dueno)
    check("el mapa de textos responde", st == 200, str(st))
    if st == 200:
        textos = r["data"]
        check("trae los diez sembrados", len(textos) == 10, "(%d)" % len(textos))
        check("y son textos, no plantillas de notificacion",
              all(isinstance(v, str) and v for v in textos.values()))

    print()
    if fallos:
        print("FALLARON %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


main()
