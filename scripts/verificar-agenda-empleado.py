# -*- coding: utf-8 -*-
"""Comprueba la agenda del empleado: que solo ve la suya y que solo la mueve
hacia adelante.

Que prueba: que un empleado lee sus citas y NO las de un companero del mismo
negocio (el aislamiento por negocio no separa a dos personas del mismo sitio),
que la fila trae con quien y de que —sin eso no sirve para trabajar—, que desde
la app se puede empezar y terminar pero no cancelar ni marcar inasistencia, que
no puede mover la cita de otro, y que al terminarla el cargo la sigue.

Como usarlo: con el stack arriba, `python scripts/verificar-agenda-empleado.py`.
Limpia lo que crea, asi que se puede repetir.
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
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"      # Ana Rivas
EMP_USER = "e2324"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
CLIENT = "c39faaf0-7eec-4312-86f1-061ddd77f3c5"
NOTA = "prueba agenda empleado"
COLEGA = "e-prueba-agenda"

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
        raise SystemExit("login %s -> %s %s" % (user, st, json.dumps(r)[:200]))
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


def agendar(token, employee_id, dia):
    """Reserva en la primera hora libre del dia. Que hora sea da igual para lo
    que se prueba, y fijar una la deja a merced de lo que ya haya en la agenda
    de las demas pruebas."""
    ultimo = (0, {})
    for hora in range(14, 22):
        inicio = "%sT%02d:00:00Z" % (dia, hora)
        st, r = http("POST", "/business/appointments", token, {
            "businessId": BIZ, "branchId": BRANCH, "employeeId": employee_id,
            "businessClientId": CLIENT, "startUtc": inicio,
            "offeringIds": [OFF], "backdated": False, "notes": NOTA})
        if st == 200:
            return st, r, inicio
        ultimo = (st, r)
    return ultimo[0], ultimo[1], None


def adelantar(cita):
    """Mueve la cita a hace dos horas, como si el dia ya hubiera llegado."""
    sql("UPDATE appointment SET StartUtc = UTC_TIMESTAMP() - INTERVAL 2 HOUR, "
        "EndUtc = UTC_TIMESTAMP() - INTERVAL 1 HOUR, LocalDate = CURDATE() "
        "WHERE Id='%s'" % cita)


def borrar_colega():
    """El companero de prueba, con su cuenta y su persona."""
    filas = sql("SELECT e.Id, e.ThirdPartyId FROM employee e WHERE e.EmployeeCode='%s'" % COLEGA)
    for emp_id, tp_id in filas:
        sql("DELETE FROM employee_balance WHERE EmployeeId='%s'" % emp_id)
        sql("DELETE FROM employee WHERE Id='%s'" % emp_id)
        users = sql("SELECT UserId FROM third_party WHERE Id='%s'" % tp_id, db="personas")
        sql("DELETE FROM third_party WHERE Id='%s'" % tp_id, db="personas")
        for (uid,) in [(u[0],) for u in users if u and u[0] not in (None, "NULL")]:
            sql("DELETE FROM user_role WHERE UserId='%s'" % uid)
            sql("DELETE FROM app_user WHERE Id='%s'" % uid)


def main():
    dueno = login("cc2085")
    empleado = login(EMP_USER)
    limpiar()
    borrar_colega()

    hoy = datetime.now()
    desde = (hoy - timedelta(days=1)).strftime("%Y-%m-%d")
    hasta = (hoy + timedelta(days=3)).strftime("%Y-%m-%d")
    dia = (hoy + timedelta(days=2)).strftime("%Y-%m-%d")

    print("=== 1) un empleado lee SU agenda, con quien y de que ===")
    # CONFIRMADA, no retroactiva: lo retroactivo nace ya COMPLETADA (se anota
    # algo prestado) y aqui se quiere probar el camino del empleado, que empieza
    # en una cita en pie.
    st, r, futuro = agendar(dueno, EMP, dia)
    check("cita creada por el dueno", st == 200, str(st) + " " + str(r.get("message", ""))[:90])
    if st != 200:
        return terminar()
    cita = r["data"]["id"]
    check("nace confirmada", r["data"]["status"] == "CONFIRMADA", r["data"]["status"])

    # Adelantar el reloj. La agenda no deja reservar en el pasado, y terminar
    # una cita que aun no ha empezado tampoco: es lo unico que una prueba no
    # puede conseguir esperando.
    adelantar(cita)

    st, r = http("GET", "/business/appointments/employee/%s?from=%s&to=%s" % (EMP, desde, hasta),
                 empleado)
    check("el empleado ve su agenda", st == 200, str(st))
    mias = [c for c in (r.get("data") or []) if c["id"] == cita]
    check("y la cita esta en ella", len(mias) == 1, "%s citas" % len(r.get("data") or []))
    if mias:
        c = mias[0]
        check("la fila dice con quien", bool(c.get("clientName")), str(c.get("clientName")))
        check("y de que", c.get("serviceName") not in (None, "", "Servicio"),
              str(c.get("serviceName")))
        check("con su codigo publico", bool(c.get("publicCode")), str(c.get("publicCode")))

    print("=== 2) la agenda de un companero NO se ve ===")
    st, r = http("POST", "/business/employees/provision", dueno, {
        "branchId": BRANCH, "username": "colega" + uuid.uuid4().hex[:6],
        "email": "colega" + uuid.uuid4().hex[:6] + "@prueba.co", "password": "Password123!"})
    check("companero dado de alta", st == 200, str(st) + " " + str(r.get("message", ""))[:120])
    if st != 200:
        return terminar()
    colega_id = r["data"]["employeeId"]

    sql("UPDATE employee SET EmployeeCode='%s' WHERE Id='%s'" % (COLEGA, colega_id))

    st, r = http("GET", "/business/appointments/employee/%s?from=%s&to=%s"
                 % (colega_id, desde, hasta), empleado)
    check("403 al pedir la agenda del companero", st == 403,
          "%s %s" % (st, str(r.get("message", ""))[:70]))

    st, r = http("GET", "/business/appointments/employee/%s?from=%s&to=%s" % (EMP, desde, hasta),
                 dueno)
    check("el dueno si ve la de su equipo", st == 200, str(st))

    print("=== 3) desde la app se empieza y se termina; nada mas ===")
    st, r = http("POST", "/business/appointments/%s/status" % cita, empleado,
                 {"status": "CANCELADA_NEGOCIO", "reason": "prueba"})
    check("403 al intentar cancelar", st == 403, "%s %s" % (st, str(r.get("message", ""))[:70]))

    st, r = http("POST", "/business/appointments/%s/status" % cita, empleado,
                 {"status": "EN_CURSO", "reason": None})
    check("puede empezarla", st == 200, "%s %s" % (st, str(r.get("message", ""))[:90]))
    check("y queda en curso", r.get("data", {}).get("status") == "EN_CURSO",
          str(r.get("data", {}).get("status")))

    st, r = http("POST", "/business/appointments/%s/status" % cita, empleado,
                 {"status": "COMPLETADA", "reason": None})
    check("puede terminarla", st == 200, "%s %s" % (st, str(r.get("message", ""))[:90]))
    check("y queda terminada", r.get("data", {}).get("status") == "COMPLETADA",
          str(r.get("data", {}).get("status")))

    print("=== 4) el movimiento queda anotado como venido de la app ===")
    hist = sql("SELECT FromStatus, ToStatus, Channel FROM appointment_history "
               "WHERE AppointmentId='%s' ORDER BY OccurredAt" % cita)
    canales = {h[2] for h in hist if h[1] in ("EN_CURSO", "COMPLETADA")}
    check("canal APK_EMPLEADO en el historial", canales == {"APK_EMPLEADO"}, str(canales))

    print("=== 5) la cita de otro no se mueve ===")
    st, r, _ = agendar(dueno, colega_id, dia)
    check("cita del companero creada", st == 200, str(st) + " " + str(r.get("message", ""))[:90])
    if st == 200:
        ajena = r["data"]["id"]
        st, r = http("POST", "/business/appointments/%s/status" % ajena, empleado,
                     {"status": "EN_CURSO", "reason": None})
        check("403 al mover la cita de otro", st == 403,
              "%s %s" % (st, str(r.get("message", ""))[:70]))

    print("=== 6) terminarla desde la app deja el cargo listo para aprobar ===")
    st, r = http("POST", "/finance/service-charges/sync?businessId=%s&from=%s&to=%s"
                 % (BIZ, desde, hasta), empleado)
    check("el empleado puede sincronizar sus cargos", st == 200,
          "%s %s" % (st, str(r.get("message", ""))[:70]))
    c = sql("SELECT Status, NetAmount FROM service_charge WHERE AppointmentId='%s'" % cita)
    check("el cargo existe", bool(c))
    if c:
        check("y esta pendiente de aprobar", c[0][0] == "PENDING", c[0][0])

    print("=== 7) el cargo ya no se puede terminar por su cuenta ===")
    if c:
        cargo_id = sql("SELECT Id FROM service_charge WHERE AppointmentId='%s'" % cita)[0][0]
        st, r = http("POST", "/finance/service-charges/%s/complete?employeeId=%s"
                     % (cargo_id, EMP), empleado)
        # Se quito a proposito: dos sitios donde consta que el servicio se
        # presto acababan contando cosas distintas.
        check("el endpoint del cargo ya no existe", st == 404,
              "%s %s" % (st, str(r.get("message", ""))[:60]))

    return terminar()


def terminar():
    limpiar()
    borrar_colega()
    print("")
    if fallos:
        print("FALLAN %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


if __name__ == "__main__":
    main()
