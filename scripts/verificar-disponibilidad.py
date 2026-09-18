# -*- coding: utf-8 -*-
"""Comprueba el calculo de disponibilidad contra el sistema levantado.

Que prueba: que los huecos salen del horario de la sede y del turno del
empleado, que una cita o un bloqueo los quitan, que los buffers corren el
siguiente, que la duracion propia del empleado manda sobre la del catalogo, y
que la antelacion minima recorta el dia de hoy.

Como usarlo: con el stack arriba (docker compose up -d), `python
scripts/verificar-disponibilidad.py`. Monta el horario que necesita y limpia lo
que crea, asi que se puede repetir.

Los identificadores de abajo son los del juego de datos de desarrollo; para
otro entorno hay que cambiarlos. El usuario es el de las pruebas E2E.
"""
import json, subprocess, sys, urllib.request, urllib.error, uuid
from datetime import date, datetime, timedelta

GW = "http://localhost:8080"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
EMP = "3324b466-a7a5-472d-99eb-1a5e528ecbb2"
OFF = "089eb164-a3dd-4a2f-9604-5842582f1ddd"
CLIENT = "c39faaf0-7eec-4312-86f1-061ddd77f3c5"
SCHED_TYPE = "61000000-0000-0000-0000-000000000001"
SHIFT_TYPE = "60000000-0000-0000-0000-000000000001"
DAYS = ["64000000-0000-0000-0000-00000000000%d" % i for i in range(1, 8)]

# Fechas RELATIVAS, no fijas. Con dias escritos a mano el guion funcionaba
# hasta que el reloj los alcanzaba: ese dia se quedaba sin huecos —porque ya
# habia pasado— y fallaba sin que nada estuviera roto.
_HOY = date.today()
DIA_A = (_HOY + timedelta(days=7)).isoformat()     # el dia de las aserciones
DIA_B = (_HOY + timedelta(days=8)).isoformat()     # el del bloqueo de agenda
DIA_C = (_HOY + timedelta(days=9)).isoformat()     # el de "otro profesional"

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
                        "-p" + dbpw(), "-N", "-e", q],
                       capture_output=True, text=True)
    if p.returncode != 0:
        raise SystemExit("SQL: " + p.stderr)
    return [l for l in p.stdout.splitlines() if "insecure" not in l]


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


def login():
    st, r = http("POST", "/auth/login", body={"usernameOrEmail": "cc2085",
                                              "password": "Password123!"})
    if st != 200:
        raise SystemExit("login " + str(st) + " " + json.dumps(r)[:200])
    return r["data"]["tokens"]["accessToken"]


# ---------------------------------------------------------------- montaje
def montar_horario():
    """Sede abierta 08:00-18:00 los siete dias, y el empleado a turno completo."""
    sql("DELETE FROM saas_db.employee_shift_assignment WHERE EmployeeId='%s'" % EMP)
    sql("DELETE FROM saas_db.branch_schedule_shift WHERE BranchScheduleId IN "
        "(SELECT Id FROM saas_db.branch_schedule WHERE BranchId='%s')" % BRANCH)
    sql("DELETE FROM saas_db.branch_schedule WHERE BranchId='%s'" % BRANCH)

    sid = str(uuid.uuid4())
    sql("INSERT INTO saas_db.branch_schedule "
        "(Id,BranchId,BusinessScheduleId,ScheduleTypeId,Name,ValidFrom,ValidTo,"
        " Enabled,Visible,AuditDate,CreatedDate) VALUES "
        "('%s','%s',NULL,'%s','Horario prueba','2026-01-01 00:00:00',NULL,1,1,NOW(6),NOW(6))"
        % (sid, BRANCH, SCHED_TYPE))

    for i, day in enumerate(DAYS):
        tid = str(uuid.uuid4())
        sql("INSERT INTO saas_db.branch_schedule_shift "
            "(Id,BranchScheduleId,ShiftTypeId,DayOfWeekId,StartTime,EndTime,DisplayOrder,"
            " Enabled,Visible,AuditDate,CreatedDate) VALUES "
            "('%s','%s','%s','%s','08:00:00','18:00:00',%d,1,1,NOW(6),NOW(6))"
            % (tid, sid, SHIFT_TYPE, day, i))
        sql("INSERT INTO saas_db.employee_shift_assignment "
            "(Id,EmployeeId,BranchScheduleShiftId,IsFullShift,CustomStartTime,CustomEndTime,"
            " StatusId,ValidFrom,ValidTo,Enabled,Visible,AuditDate,CreatedDate) VALUES "
            "('%s','%s','%s',1,NULL,NULL,NULL,'2026-01-01 00:00:00',NULL,1,1,NOW(6),NOW(6))"
            % (str(uuid.uuid4()), EMP, tid))


def disponibilidad(token, dia, hasta=None, empleado=None):
    p = ("/business/availability?businessId=%s&branchId=%s&offeringIds=%s&from=%s&to=%s"
         % (BIZ, BRANCH, OFF, dia, hasta or dia))
    if empleado:
        p += "&employeeId=" + empleado
    st, r = http("GET", p, token)
    if st != 200:
        raise SystemExit("disponibilidad " + str(st) + " " + json.dumps(r)[:300])
    return r["data"]


def horas(data):
    """Las horas locales de inicio, como HH:MM."""
    from datetime import timezone
    out = []
    for s in data["slots"]:
        t = datetime.fromisoformat(s["startUtc"].replace("Z", "+00:00"))
        out.append((t - timedelta(hours=5)).strftime("%H:%M"))
    return out


def main():
    token = login()
    # La prueba crea una cita y un bloqueo; se limpian antes para poder
    # repetirla tantas veces como haga falta.
    sql("DELETE FROM saas_db.appointment_service WHERE AppointmentId IN "
        "(SELECT Id FROM saas_db.appointment WHERE Notes='prueba disponibilidad')")
    sql("DELETE FROM saas_db.appointment_history WHERE AppointmentId IN "
        "(SELECT Id FROM saas_db.appointment WHERE Notes='prueba disponibilidad')")
    sql("DELETE FROM saas_db.appointment_notification WHERE AppointmentId IN "
        "(SELECT Id FROM saas_db.appointment WHERE Notes='prueba disponibilidad')")
    sql("DELETE FROM saas_db.appointment WHERE Notes='prueba disponibilidad'")
    sql("DELETE FROM saas_db.agenda_exception WHERE Reason IN ('Capacitación','Vacaciones')")

    print("=== 1) sin servicios asignados al empleado, no hay disponibilidad ===")
    sql("DELETE FROM saas_db.employee_offering WHERE EmployeeId='%s'" % EMP)
    montar_horario()
    d = disponibilidad(token, DIA_A)
    check("nadie presta el servicio -> 0 huecos", len(d["slots"]) == 0,
          "(%d)" % len(d["slots"]))
    check("devuelve la zona del negocio", d["timeZone"] == "America/Bogota", d["timeZone"])

    print("=== 2) con el servicio asignado, salen los huecos del horario ===")
    st, r = http("PUT", "/business/employees/%s/offerings" % EMP, token,
                 [{"offeringId": OFF}])
    check("PUT servicios del empleado", st == 200, str(st))

    # La cita de las 14:00 la crea ESTE guion. Antes venia sembrada en un dia
    # escrito a mano, y al pasar las fechas a relativas ese dia dejo de tenerla:
    # la prueba media 39 huecos en vez de 36 y fallaba sin que nada estuviera mal.
    st, _ = http("POST", "/business/appointments", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
        "businessClientId": CLIENT, "startUtc": DIA_A + "T19:00:00Z",
        "offeringIds": [OFF], "backdated": False, "notes": "prueba disponibilidad"})
    check("la cita de las 14:00 se crea", st == 200, str(st))

    d = disponibilidad(token, DIA_A)
    hs = horas(d)
    # 08:00..17:30 cada 15 min = 39, menos los 3 que pisan la cita de las 14:00
    check("primer hueco a las 08:00", hs and hs[0] == "08:00", hs[0] if hs else "-")
    check("ultimo hueco a las 17:30 (el servicio cabe entero)",
          hs and hs[-1] == "17:30", hs[-1] if hs else "-")
    check("huecos alineados al cuarto de hora",
          all(int(h[3:]) % 15 == 0 for h in hs))
    check("la cita de las 14:00 se quita a si misma y a lo que la pisa",
          "14:00" not in hs and "13:45" not in hs and "14:15" not in hs)
    check("13:30 sigue libre (termina justo cuando empieza la cita)", "13:30" in hs)
    check("14:30 sigue libre (empieza justo cuando acaba)", "14:30" in hs)
    check("total 36 huecos", len(hs) == 36, "(%d)" % len(hs))

    print("=== 3) reservar un hueco lo quita de la lista ===")
    st, r = http("POST", "/business/appointments", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
        "businessClientId": CLIENT, "startUtc": DIA_A + "T20:00:00Z",
        "offeringIds": [OFF], "backdated": False, "notes": "prueba disponibilidad"})
    check("reserva de las 15:00 aceptada", st == 200, str(st) + " " + json.dumps(r)[:160])
    hs2 = horas(disponibilidad(token, DIA_A))
    check("15:00 ya no aparece", "15:00" not in hs2)
    check("14:45 tampoco (se solaparia)", "14:45" not in hs2)
    check("15:30 sigue disponible", "15:30" in hs2)
    check("tres huecos menos que antes", len(hs2) == len(hs) - 3,
          "(%d -> %d)" % (len(hs), len(hs2)))

    print("=== 4) un bloqueo de agenda resta esas horas ===")
    st, r = http("POST", "/business/agenda-exceptions", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
        "startUtc": DIA_B + "T15:00:00Z", "endUtc": DIA_B + "T17:00:00Z",
        "kind": "BLOCK", "reason": "Capacitación"})
    check("bloqueo 10:00-12:00 creado", st == 200, str(st) + " " + json.dumps(r)[:160])
    bloqueo = r.get("data", {}).get("id")
    hs3 = horas(disponibilidad(token, DIA_B))
    check("10:00 bloqueado", "10:00" not in hs3)
    check("11:45 bloqueado", "11:45" not in hs3)
    check("09:45 bloqueado (se metia dentro)", "09:45" not in hs3)
    check("09:30 libre", "09:30" in hs3)
    check("12:00 libre otra vez", "12:00" in hs3)

    print("=== 5) el bloqueo no se puede poner encima de citas vendidas ===")
    st, r = http("POST", "/business/agenda-exceptions", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
        "startUtc": DIA_A + "T19:00:00Z", "endUtc": DIA_A + "T21:00:00Z",
        "kind": "VACATION", "reason": "Vacaciones"})
    check("rechazado con motivo entendible", st >= 400 and "cita" in json.dumps(r),
          str(st) + " " + str(r.get("message", ""))[:120])

    print("=== 6) la antelacion minima recorta el dia de hoy ===")
    hoy = datetime.now()
    d_hoy = disponibilidad(token, hoy.strftime("%Y-%m-%d"))
    limite = hoy + timedelta(minutes=30)
    tempranos = [s for s in d_hoy["slots"]
                 if datetime.fromisoformat(s["startUtc"].replace("Z", "+00:00")).timestamp()
                 < limite.timestamp()]
    check("ningun hueco de hoy dentro de los proximos 30 min", len(tempranos) == 0,
          "(%d)" % len(tempranos))

    print("=== 7) filtrar por profesional ===")
    d_otro = disponibilidad(token, DIA_C,
                            empleado="00000000-0000-0000-0000-000000000999")
    check("un empleado que no es de la sede no da huecos", len(d_otro["slots"]) == 0)
    d_mio = disponibilidad(token, DIA_C, empleado=EMP)
    check("el empleado correcto si", len(d_mio["slots"]) > 0,
          "(%d)" % len(d_mio["slots"]))

    print("=== 8) rango absurdo rechazado ===")
    # Un ano entero: el servidor tiene que negarse, no ponerse a calcular.
    un_ano_despues = (date.today() + timedelta(days=365)).isoformat()
    st, r = http("GET", "/business/availability?businessId=%s&branchId=%s&offeringIds=%s"
                        "&from=%s&to=%s" % (BIZ, BRANCH, OFF, DIA_A, un_ano_despues), token)
    check("un ano de golpe se rechaza", st >= 400, str(st))

    print("=== 9) los buffers de limpieza corren el hueco siguiente ===")
    st, r = http("PUT", "/business/booking-policies?businessId=" + BIZ, token,
                 {"bufferAfterMinutes": 10})
    check("politica guardada", st == 200, str(st) + " " + str(r.get("message", ""))[:120])
    hs4 = horas(disponibilidad(token, DIA_A))
    # La cita de 15:00-15:30 pasa a ocupar hasta las 15:40, asi que las 15:30
    # ya no cabe y el siguiente hueco de la rejilla es a las 15:45.
    check("15:30 deja de estar libre por la limpieza", "15:30" not in hs4)
    check("15:45 es el siguiente", "15:45" in hs4)
    check("08:00 sigue siendo el primero (no hay nada anterior que limpiar)",
          hs4 and hs4[0] == "08:00", hs4[0] if hs4 else "-")

    print("=== 10) la duracion propia del empleado manda sobre la del catalogo ===")
    http("PUT", "/business/booking-policies?businessId=" + BIZ, token,
         {"bufferAfterMinutes": 0})
    st, r = http("PUT", "/business/employees/%s/offerings" % EMP, token,
                 [{"offeringId": OFF, "durationMinutes": 45}])
    check("servicio con duracion propia guardado", st == 200, str(st))
    d5 = disponibilidad(token, DIA_C)
    hs5 = horas(d5)
    check("el ultimo hueco se adelanta a las 17:15", hs5 and hs5[-1] == "17:15",
          hs5[-1] if hs5 else "-")
    check("y el hueco dura 45 minutos", d5["slots"][0]["minutes"] == 45,
          str(d5["slots"][0]["minutes"]))

    # limpieza de lo que crea la prueba
    http("PUT", "/business/employees/%s/offerings" % EMP, token, [{"offeringId": OFF}])
    if bloqueo:
        http("DELETE", "/business/agenda-exceptions/" + bloqueo, token)

    print()
    if fallos:
        print("FALLARON %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


main()
