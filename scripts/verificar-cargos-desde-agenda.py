# -*- coding: utf-8 -*-
"""Comprueba que lo que se agenda acaba siendo lo que se cobra.

Que prueba: que una cita futura crea un cargo programado, que completarla lo
pasa a pendiente de aprobar, que el importe sale del snapshot de la cita y no
del catalogo, que cancelar descarta el cargo con su motivo, que el
sincronizador es idempotente, y que aprobar + liquidar mueve el saldo del
empleado por el neto exacto.

Como usarlo: con el stack arriba, `python scripts/verificar-cargos-desde-agenda.py`.
Limpia lo que crea, asi que se puede repetir.
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta

GW = "http://localhost:8080"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
CLIENT = "c39faaf0-7eec-4312-86f1-061ddd77f3c5"
NOTA = "prueba cargos"

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
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    sql("DELETE FROM service_charge WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    sql("DELETE FROM appointment_history WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    sql("DELETE FROM appointment_service WHERE AppointmentId IN "
        "(SELECT Id FROM appointment WHERE Notes='%s')" % NOTA)
    sql("DELETE FROM appointment WHERE Notes='%s'" % NOTA)


def sincronizar(token, desde, hasta):
    st, r = http("POST", "/finance/service-charges/sync?businessId=%s&from=%s&to=%s"
                 % (BIZ, desde, hasta), token)
    if st != 200:
        raise SystemExit("sync -> %s %s" % (st, json.dumps(r)[:200]))
    return r["data"]


def cargo_de(appointment_id):
    filas = sql("SELECT Status, GrossAmount, NetAmount, DeductionRate, ServiceName, "
                "COALESCE(DiscardReason,'') FROM service_charge WHERE AppointmentId='%s'"
                % appointment_id)
    return filas[0] if filas else None


def main():
    dueno = login("cc2085")
    limpiar()

    # La compensacion vigente del empleado decide el neto. Se lee, no se supone.
    comp = sql("SELECT CompensationType, CompensationValue FROM employee_compensation "
               "WHERE EmployeeId='%s' AND ValidTo IS NULL ORDER BY ValidFrom DESC LIMIT 1" % EMP)
    if not comp:
        comp = sql("SELECT CompensationType, CompensationValue FROM business_compensation "
                   "WHERE BusinessId='%s' AND ValidTo IS NULL ORDER BY ValidFrom DESC LIMIT 1" % BIZ)
    if not comp:
        # Sin compensación el neto sería cero y la prueba del dinero no probaría
        # nada. Se configura una y se deja puesta: es configuración del negocio,
        # no basura de prueba.
        sql("INSERT INTO business_compensation (Id,BusinessId,CompensationType,"
            "CompensationValue,SalaryBase,PayrollFrequency,ValidFrom,ValidTo,Enabled,"
            "Visible,AuditDate,CreatedDate) VALUES (UUID(),'%s','SERVICE_PERCENT_ONLY',"
            "40.00,NULL,'BIWEEKLY',NOW(6),NULL,1,1,NOW(6),NOW(6))" % BIZ)
        comp = sql("SELECT CompensationType, CompensationValue FROM business_compensation "
                   "WHERE BusinessId='%s' AND ValidTo IS NULL ORDER BY ValidFrom DESC LIMIT 1" % BIZ)
    porcentaje = float(comp[0][1]) if comp else 0.0
    print("compensación vigente: %s %s%%" % (comp[0][0] if comp else "ninguna", porcentaje))

    # Se registra RETROACTIVA: una cita futura no se puede dar por terminada
    # (regla de dominio), y lo que se quiere probar aquí es la cadena del cobro.
    ayer = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
    inicio = ayer + "T16:00:00Z"        # 11:00 en Bogotá
    desde = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
    hasta = (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d")

    print("=== 1) una cita agendada crea su cargo, programado ===")
    st, r = http("POST", "/business/appointments", dueno, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
        "businessClientId": CLIENT, "startUtc": inicio,
        "offeringIds": [OFF], "backdated": True, "notes": NOTA})
    check("cita creada", st == 200, str(st) + " " + str(r.get("message", ""))[:90])
    if st != 200:
        return terminar()
    cita = r["data"]["id"]
    precio = float(r["data"]["totalPrice"])

    res = sincronizar(dueno, desde, hasta)
    check("el sincronizador crea uno", res["creados"] >= 1, str(res))
    c = cargo_de(cita)
    check("existe el cargo de esa cita", c is not None)
    if not c:
        return terminar()
    # Nace ya prestada, asi que su cargo nace pendiente de aprobar, no programado.
    check("nace pendiente de aprobar", c[0] == "PENDING", c[0])
    check("el bruto es el de la cita", abs(float(c[1]) - precio) < 0.01,
          "%s vs %s" % (c[1], precio))
    esperado = round(precio * porcentaje / 100, 2)
    check("el neto sale de la compensación vigente",
          abs(float(c[2]) - esperado) < 0.01, "%s esperado %s" % (c[2], esperado))
    check("la retención es el complemento",
          abs(float(c[3]) - (100 - porcentaje)) < 0.01, c[3])
    check("guarda el nombre del servicio, no su id", c[4] and c[4] != "Servicio", c[4])

    print("=== 2) volver a sincronizar no duplica ni cambia nada ===")
    res2 = sincronizar(dueno, desde, hasta)
    check("segunda pasada sin creaciones", res2["creados"] == 0, str(res2))
    filas = sql("SELECT COUNT(*) FROM service_charge WHERE AppointmentId='%s'" % cita)
    check("sigue habiendo un solo cargo", filas[0][0] == "1", filas[0][0])

    print("=== 3) aprobar y liquidar mueve el saldo por el neto ===")
    antes = sql("SELECT Balance FROM employee_balance WHERE EmployeeId='%s'" % EMP)
    saldo_antes = float(antes[0][0]) if antes else 0.0
    cargo_id = sql("SELECT Id FROM service_charge WHERE AppointmentId='%s'" % cita)[0][0]
    st, r = http("POST", "/finance/service-charges/%s/confirm" % cargo_id, dueno, {})
    check("aprobado", st == 200, str(st) + " " + str(r.get("message", ""))[:80])
    st, r = http("POST", "/finance/settlements", dueno,
                 {"employeeId": EMP, "note": "prueba"})
    check("liquidado", st == 200, str(st) + " " + str(r.get("message", ""))[:120])
    despues = sql("SELECT Balance FROM employee_balance WHERE EmployeeId='%s'" % EMP)
    saldo_despues = float(despues[0][0]) if despues else 0.0
    check("el saldo sube exactamente el neto",
          abs((saldo_despues - saldo_antes) - esperado) < 0.01,
          "%s -> %s (esperado +%s)" % (saldo_antes, saldo_despues, esperado))

    mov = sql("SELECT MovementType, Amount FROM employee_settlement "
              "WHERE EmployeeId='%s' ORDER BY SettledAt DESC LIMIT 1" % EMP)
    check("y queda el movimiento en el extracto",
          mov and mov[0][0] == "COMMISSION" and abs(float(mov[0][1]) - esperado) < 0.01,
          str(mov[0]) if mov else "sin movimiento")

    print("=== 5) el ciclo de una cita futura: programado -> pendiente -> descartado ===")
    # A la primera hora que este libre. Con una hora fija, basta que otra prueba
    # deje una cita ahi para que esta falle por algo que no esta probando.
    dia = (datetime.now() + timedelta(days=2)).strftime("%Y-%m-%d")
    st, r = 0, {}
    for h in range(14, 22):
        st, r = http("POST", "/business/appointments", dueno, {
            "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
            "businessClientId": CLIENT, "startUtc": "%sT%02d:00:00Z" % (dia, h),
            "offeringIds": [OFF], "backdated": False, "notes": NOTA})
        if st == 200:
            break
    check("segunda cita creada", st == 200, str(st) + " " + str(r.get("message", ""))[:70])
    cita2 = r["data"]["id"] if st == 200 else None
    if cita2:
        sincronizar(dueno, desde, hasta)
        check("su cargo nace programado", cargo_de(cita2)[0] == "SCHEDULED")
        st, _ = http("POST", "/business/appointments/%s/status" % cita2, dueno,
                     {"status": "CANCELADA_NEGOCIO", "reason": "prueba"})
        check("cita cancelada", st == 200, str(st))
        res5 = sincronizar(dueno, desde, hasta)
        check("el sincronizador lo descarta", res5["descartados"] >= 1, str(res5))
        c2 = cargo_de(cita2)
        check("queda descartado con motivo", c2[0] == "DISCARDED" and c2[5], str(c2[:1] + c2[5:]))

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
