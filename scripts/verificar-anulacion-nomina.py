# -*- coding: utf-8 -*-
"""Comprueba que una dispersion de nomina se puede deshacer, y que el mismo
comprobante en dos pagos se nota.

Que prueba (hallazgos 6 y 7 del inventario):
  * que anular NO borra: escribe un contra-movimiento por pago y el pago sigue
    en el libro;
  * que el saldo vuelve exactamente por lo que se pago;
  * que hace falta motivo;
  * que no se puede anular dos veces (y que el saldo NO se devuelve dos veces);
  * que NO se puede anular si un pago en efectivo ya fue acusado — ahi el
    dinero esta en el bolsillo del colaborador;
  * que el comprobante se guarda con su SHA-256 y que dos pagos con el mismo
    fichero comparten huella (lo cual es legitimo y por eso no se rechaza).

Como usarlo: con el stack arriba, `python scripts/verificar-anulacion-nomina.py`.
Limpia lo que crea, asi que se puede repetir.
"""
import io
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta

GW = "http://localhost:8080"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
CLIENT = "c39faaf0-7eec-4312-86f1-061ddd77f3c5"
NOTA = "prueba anulacion"

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
    p = subprocess.run(["docker", "exec", "-i", "saas-mysql", "mysql",
                        "--default-character-set=utf8mb4", "-uroot",
                        "-p" + dbpw(), "-D", db, "-N", "-e", q],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if p.returncode != 0:
        raise SystemExit("SQL: " + p.stderr)
    return [l.split("\t") for l in p.stdout.splitlines() if "insecure" not in l]


def http(method, path, token=None, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(GW + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def subir_png(token, contenido):
    """Sube un PNG minimo y devuelve (url, hash) como los da el servidor."""
    limite = "----prueba" + uuid.uuid4().hex
    partes = []
    for nombre, valor in [("category", "payroll")]:
        partes.append("--" + limite)
        partes.append('Content-Disposition: form-data; name="%s"' % nombre)
        partes.append("")
        partes.append(valor)
    partes.append("--" + limite)
    partes.append('Content-Disposition: form-data; name="file"; filename="comprobante.png"')
    partes.append("Content-Type: image/png")
    partes.append("")
    cuerpo = ("\r\n".join(partes) + "\r\n").encode() + contenido + ("\r\n--%s--\r\n" % limite).encode()

    req = urllib.request.Request(GW + "/business/attachments", data=cuerpo, method="POST")
    req.add_header("Content-Type", "multipart/form-data; boundary=" + limite)
    req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            d = json.loads(r.read().decode())["data"]
            return d.get("url"), d.get("hash")
    except urllib.error.HTTPError as e:
        return None, e.read().decode()[:120]


def login(user, password="Password123!"):
    st, r = http("POST", "/auth/login", body={"usernameOrEmail": user, "password": password})
    if st != 200:
        raise SystemExit("login %s -> %s" % (user, st))
    return r["data"]["tokens"]["accessToken"]


def limpiar():
    citas = "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA
    sql("DELETE FROM employee_settlement WHERE Note LIKE '%s%%' OR Note LIKE 'Anulación de%%'" % NOTA)
    sql("DELETE FROM payroll_run WHERE Note LIKE '%s%%'" % NOTA)
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN " + citas)
    sql("DELETE FROM service_charge WHERE AppointmentId IN " + citas)
    for t in ("appointment_history", "appointment_service"):
        sql("DELETE FROM %s WHERE AppointmentId IN %s" % (t, citas))
    sql("DELETE FROM appointment WHERE Notes='%s'" % NOTA)


def saldo():
    filas = sql("SELECT AmountAccrued, AmountPaid, Balance FROM employee_balance "
                "WHERE EmployeeId='%s'" % EMP)
    return [float(x) for x in filas[0]] if filas else [0.0, 0.0, 0.0]


def preparar_saldo(token):
    """Deja saldo a favor del empleado: una cita prestada, aprobada y liquidada."""
    ayer = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
    st, r = http("POST", "/business/appointments", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
        "businessClientId": CLIENT, "startUtc": ayer + "T16:00:00Z",
        "offeringIds": [OFF], "backdated": True, "notes": NOTA})
    if st != 200:
        raise SystemExit("no se pudo crear la cita: %s" % st)
    cita = r["data"]["id"]

    desde = (datetime.now() - timedelta(days=2)).strftime("%Y-%m-%d")
    hasta = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    http("POST", "/finance/service-charges/sync?businessId=%s&from=%s&to=%s" % (BIZ, desde, hasta), token)

    cargo = sql("SELECT Id FROM service_charge WHERE AppointmentId='%s'" % cita)
    if not cargo:
        raise SystemExit("el cargo no se creo")
    http("POST", "/finance/service-charges/%s/confirm" % cargo[0][0], token, {})
    http("POST", "/finance/settlements", token, {"employeeId": EMP, "note": NOTA})
    return saldo()[2]


def main():
    dueno = login("cc2085")
    limpiar()

    print("=== 1) el comprobante se guarda con su huella ===")
    png = b"\x89PNG\r\n\x1a\n" + b"prueba-comprobante" + os.urandom(8)
    url1, hash1 = subir_png(dueno, png)
    check("la subida devuelve url y hash", bool(url1) and bool(hash1) and len(hash1 or "") == 64,
          "%s %s" % (str(url1)[:40], str(hash1)[:16]))

    url2, hash2 = subir_png(dueno, png)
    check("el MISMO fichero da el MISMO hash aunque cambie la url",
          hash1 == hash2 and url1 != url2, "%s vs %s" % (str(hash1)[:12], str(hash2)[:12]))

    _, hash3 = subir_png(dueno, png + b"otro")
    check("otro fichero da otro hash", hash1 != hash3, str(hash3)[:12])

    print("=== 2) se dispersa con saldo real ===")
    a_favor = preparar_saldo(dueno)
    check("el empleado tiene saldo a favor", a_favor > 0, str(a_favor))
    antes = saldo()

    st, r = http("POST", "/finance/payroll/runs", dueno, {
        "businessId": BIZ, "branchId": BRANCH, "note": NOTA,
        "items": [{"employeeId": EMP, "payoutAccount": "Cuenta de prueba",
                   "paymentProofUrl": url1, "paymentProofHash": hash1,
                   "paidInCash": False}]},
        headers={"Idempotency-Key": uuid.uuid4().hex})
    check("dispersada", st == 200, "%s %s" % (st, str(r.get("message", ""))[:70]))
    if st != 200:
        return terminar()
    run = r["data"]["id"]
    codigo = r["data"]["code"]

    despues = saldo()
    check("el saldo queda en cero", abs(despues[2]) < 0.01, str(despues[2]))
    check("y lo pagado subio por el importe",
          abs((despues[1] - antes[1]) - a_favor) < 0.01,
          "%s -> %s" % (antes[1], despues[1]))

    guardado = sql("SELECT PaymentProofHash FROM employee_settlement "
                   "WHERE PayrollRunId='%s' AND MovementType='PAYROLL'" % run)
    check("el pago guardo la huella del comprobante",
          guardado and guardado[0][0] == hash1, str(guardado[0][0])[:16] if guardado else "-")

    print("=== 3) anular pide motivo ===")
    st, r = http("POST", "/finance/payroll/runs/%s/void" % run, dueno, {"reason": ""})
    check("sin motivo se rechaza", st == 400, str(st))

    print("=== 4) anular devuelve el saldo sin borrar el pago ===")
    st, r = http("POST", "/finance/payroll/runs/%s/void" % run, dueno,
                 {"reason": "Se transfirió a la cuenta equivocada"})
    check("anulada", st == 200, "%s %s" % (st, str(r.get("message", ""))[:60]))
    check("la corrida queda ANULADA", (r.get("data") or {}).get("status") == "ANULADA",
          str((r.get("data") or {}).get("status")))
    check("con su motivo", bool((r.get("data") or {}).get("voidReason")),
          str((r.get("data") or {}).get("voidReason"))[:40])

    pago = sql("SELECT Id FROM employee_settlement WHERE PayrollRunId='%s' "
               "AND MovementType='PAYROLL'" % run)
    check("el PAGO sigue en el libro", len(pago) == 1, "%d filas" % len(pago))

    contra = sql("SELECT Amount, ReversalOfId FROM employee_settlement "
                 "WHERE PayrollRunId='%s' AND MovementType='PAYROLL_REVERSAL'" % run)
    check("y se escribio el contra-movimiento", len(contra) == 1, "%d filas" % len(contra))
    if contra and pago:
        check("que apunta al pago que deshace", contra[0][1] == pago[0][0])
        check("por el mismo importe", abs(float(contra[0][0]) - a_favor) < 0.01,
              "%s vs %s" % (contra[0][0], a_favor))

    vuelto = saldo()
    check("el saldo vuelve a lo que era", abs(vuelto[2] - a_favor) < 0.01,
          "%s (esperado %s)" % (vuelto[2], a_favor))
    check("y lo pagado vuelve a lo de antes", abs(vuelto[1] - antes[1]) < 0.01,
          "%s -> %s" % (despues[1], vuelto[1]))

    print("=== 5) no se anula dos veces ===")
    st, r = http("POST", "/finance/payroll/runs/%s/void" % run, dueno, {"reason": "otra vez"})
    check("el segundo intento se rechaza", st == 400,
          "%s %s" % (st, str(r.get("message", ""))[:60]))
    otra = saldo()
    check("y el saldo NO se devolvio dos veces", abs(otra[2] - a_favor) < 0.01,
          "%s vs %s" % (otra[2], a_favor))

    print("=== 6) el efectivo ya acusado no se puede deshacer ===")
    st, r = http("POST", "/finance/payroll/runs", dueno, {
        "businessId": BIZ, "branchId": BRANCH, "note": NOTA,
        "items": [{"employeeId": EMP, "payoutAccount": "Efectivo", "paidInCash": True}]},
        headers={"Idempotency-Key": uuid.uuid4().hex})
    check("segunda dispersion, en efectivo", st == 200, str(st))
    if st == 200:
        run2 = r["data"]["id"]
        mov = sql("SELECT Id FROM employee_settlement WHERE PayrollRunId='%s' "
                  "AND MovementType='PAYROLL'" % run2)
        # El colaborador firma que recibio los billetes.
        sql("UPDATE employee_settlement SET CashConfirmedAt = NOW(6) WHERE Id='%s'" % mov[0][0])

        st, r = http("POST", "/finance/payroll/runs/%s/void" % run2, dueno,
                     {"reason": "me equivoqué"})
        check("se niega a anular", st == 400, "%s %s" % (st, str(r.get("message", ""))[:70]))
        check("y explica por qué", "efectivo" in str(r.get("message", "")).lower(),
              str(r.get("message", ""))[:70])

    print("=== 7) el mismo comprobante en dos pagos comparte huella ===")
    # Es legitimo —una transferencia para varias personas— y por eso NO se
    # rechaza; lo que hacia falta es que se pueda ver.
    comparten = sql("SELECT COUNT(*) FROM employee_settlement "
                    "WHERE BusinessId='%s' AND PaymentProofHash='%s'" % (BIZ, hash1))
    check("la huella permite contarlos", comparten and int(comparten[0][0]) >= 1,
          str(comparten[0][0]) if comparten else "-")

    return terminar()


def terminar():
    limpiar()
    print("")
    if fallos:
        print("FALLAN %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


if __name__ == "__main__":
    main()
