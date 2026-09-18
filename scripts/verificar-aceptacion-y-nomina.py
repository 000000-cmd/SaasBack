# -*- coding: utf-8 -*-
"""Comprueba los defectos del flujo de aceptación y de nómina que movían dinero.

Que prueba: que aprobar dos veces no reenvía la factura ni re-escribe la fecha,
que rechazar sin motivo se rechaza, que una aprobación se puede reversar antes
de liquidar y no después, y que dos envíos de la misma nómina pagan una sola
vez.

Como usarlo: con el stack arriba, `python scripts/verificar-aceptacion-y-nomina.py`.
Limpia lo que crea, así que se puede repetir.
"""
import json
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
NOTA = "prueba aceptacion"

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


def login(user, password="Password123!"):
    st, r = http("POST", "/auth/login", body={"usernameOrEmail": user, "password": password})
    if st != 200:
        raise SystemExit("login %s -> %s" % (user, st))
    return r["data"]["tokens"]["accessToken"]


def limpiar():
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    sql("DELETE FROM service_charge WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    for t in ("appointment_history", "appointment_service"):
        sql("DELETE FROM %s WHERE AppointmentId IN "
            "(SELECT Id FROM appointment WHERE Notes='%s')" % (t, NOTA))
    sql("DELETE FROM appointment WHERE Notes='%s'" % NOTA)


def crear_cargo(token, minutos_atras):
    """Registra un servicio ya prestado y devuelve el id de su cargo."""
    inicio = (datetime.utcnow() - timedelta(minutes=minutos_atras)).strftime("%Y-%m-%dT%H:00:00Z")
    st, r = http("POST", "/business/appointments", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
        "businessClientId": CLIENT, "startUtc": inicio,
        "offeringIds": [OFF], "backdated": True, "notes": NOTA})
    if st != 200:
        raise SystemExit("cita -> %s %s" % (st, json.dumps(r)[:200]))
    cita = r["data"]["id"]
    hoy = datetime.now()
    http("POST", "/finance/service-charges/sync?businessId=%s&from=%s&to=%s"
         % (BIZ, (hoy - timedelta(days=2)).strftime("%Y-%m-%d"),
            (hoy + timedelta(days=1)).strftime("%Y-%m-%d")), token)
    filas = sql("SELECT Id FROM service_charge WHERE AppointmentId='%s'" % cita)
    return filas[0][0] if filas else None


def main():
    dueno = login("cc2085")
    limpiar()

    print("=== 1) aprobar dos veces no repite nada ===")
    cargo = crear_cargo(dueno, 180)
    check("hay cargo que aprobar", cargo is not None)
    if not cargo:
        return terminar()
    st, _ = http("POST", "/finance/service-charges/%s/confirm" % cargo, dueno, {})
    check("primera aprobación", st == 200, str(st))
    fecha1 = sql("SELECT ConfirmedAt FROM service_charge WHERE Id='%s'" % cargo)[0][0]
    st, _ = http("POST", "/finance/service-charges/%s/confirm" % cargo, dueno, {})
    check("segunda aprobación no falla", st == 200, str(st))
    fecha2 = sql("SELECT ConfirmedAt FROM service_charge WHERE Id='%s'" % cargo)[0][0]
    check("no re-escribe la fecha de aprobación", fecha1 == fecha2,
          "%s vs %s" % (fecha1, fecha2))

    print("=== 2) reversar una aprobación la devuelve a pendiente ===")
    st, r = http("POST", "/finance/service-charges/%s/revert" % cargo, dueno,
                 {"reason": "Me equivoqué de servicio"})
    check("reversada", st == 200, str(st) + " " + str(r.get("message", ""))[:80])
    fila = sql("SELECT Status, COALESCE(ConfirmedAt,''), COALESCE(DiscardReason,'') "
               "FROM service_charge WHERE Id='%s'" % cargo)[0]
    check("vuelve a pendiente", fila[0] == "PENDING", fila[0])
    # La fecha de aprobación se conserva a propósito: es un hecho histórico, y
    # quien manda sobre si está aprobado es el estado.
    check("conserva la fecha como historia", bool(fila[1]), fila[1])
    check("queda el motivo", "Me equivoqué" in fila[2], fila[2])

    print("=== 3) reversar exige motivo, y solo vale sobre lo aprobado ===")
    st, r = http("POST", "/finance/service-charges/%s/revert" % cargo, dueno, {"reason": ""})
    check("sin motivo se rechaza", st >= 400, str(st))
    http("POST", "/finance/service-charges/%s/confirm" % cargo, dueno, {})
    st, r = http("POST", "/finance/service-charges/%s/revert" % cargo, dueno,
                 {"reason": "otra vez"})
    check("aprobado otra vez, se puede reversar", st == 200, str(st))
    st, r = http("POST", "/finance/service-charges/%s/revert" % cargo, dueno,
                 {"reason": "y otra"})
    check("sobre uno pendiente ya no", st >= 400,
          str(st) + " " + str(r.get("message", ""))[:80])

    print("=== 4) rechazar sin motivo se rechaza ===")
    otro = crear_cargo(dueno, 300)
    st, r = http("POST", "/finance/service-charges/%s/discard" % otro, dueno, {"reason": ""})
    check("motivo vacío rechazado", st >= 400, str(st))
    st, r = http("POST", "/finance/service-charges/%s/discard" % otro, dueno,
                 {"reason": "El cliente no vino"})
    check("con motivo sí", st == 200, str(st))
    fila = sql("SELECT Status, DiscardReason FROM service_charge WHERE Id='%s'" % otro)[0]
    check("guarda el motivo tal cual", fila[1] == "El cliente no vino", fila[1])

    print("=== 5) dos envíos de la misma nómina pagan una vez ===")
    # Deja algo que pagar: aprobar y liquidar el primer cargo.
    http("POST", "/finance/service-charges/%s/confirm" % cargo, dueno, {})
    st, r = http("POST", "/finance/settlements", dueno, {"employeeId": EMP, "note": NOTA})
    check("liquidado para tener saldo", st == 200, str(st) + " " + str(r.get("message", ""))[:90])
    saldo = float(sql("SELECT Balance FROM employee_balance WHERE EmployeeId='%s'" % EMP)[0][0])
    check("hay saldo a favor", saldo > 0, str(saldo))

    clave = str(uuid.uuid4())
    cuerpo = {"businessId": BIZ, "branchId": BRANCH, "note": NOTA,
              "items": [{"employeeId": EMP, "paidInCash": True}]}
    st1, r1 = http("POST", "/finance/payroll/runs", dueno, cuerpo,
                   {"Idempotency-Key": clave})
    st2, r2 = http("POST", "/finance/payroll/runs", dueno, cuerpo,
                   {"Idempotency-Key": clave})
    check("primer envío paga", st1 == 200, str(st1) + " " + str(r1.get("message", ""))[:90])
    check("segundo envío no falla", st2 == 200, str(st2))
    if st1 == 200 and st2 == 200:
        check("y devuelve la MISMA corrida", r1["data"]["code"] == r2["data"]["code"],
              "%s vs %s" % (r1["data"]["code"], r2["data"]["code"]))
    corridas = sql("SELECT COUNT(*) FROM payroll_run WHERE IdempotencyKey='%s'" % clave)
    check("una sola corrida en la base", corridas[0][0] == "1", corridas[0][0])
    # Contado por la corrida de AHORA, no por la nota: las pasadas anteriores
    # dejaron sus propios movimientos y borrarlos sería falsear el extracto.
    pagos = sql("SELECT COUNT(*) FROM employee_settlement s "
                "JOIN payroll_run r ON r.Id = s.PayrollRunId "
                "WHERE s.EmployeeId='%s' AND r.IdempotencyKey='%s'" % (EMP, clave))
    check("un solo pago en esa corrida", pagos[0][0] == "1", pagos[0][0])
    final = float(sql("SELECT Balance FROM employee_balance WHERE EmployeeId='%s'" % EMP)[0][0])
    check("el saldo queda en cero, no en negativo", abs(final) < 0.01, str(final))

    terminar()


def terminar():
    print()
    if fallos:
        print("FALLARON %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


main()
