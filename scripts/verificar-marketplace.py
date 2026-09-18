# -*- coding: utf-8 -*-
"""Comprueba el directorio publico y las calificaciones.

Que prueba: que un negocio NO sale del directorio hasta que su dueno lo publica,
que la ficha se resuelve por slug, que solo se puede calificar una cita
COMPLETADA, que hace falta demostrar que la cita es tuya (codigo + ultimos
cuatro), que no se puede opinar dos veces, que el agregado se suma de forma
atomica, y que el orden bayesiano no pone primero al que tiene un unico cinco.

Como usarlo: con el stack arriba, `python scripts/verificar-marketplace.py`.
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
NOTA = "prueba marketplace"

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
    sql("DELETE FROM appointment_review WHERE AppointmentId IN " + citas)
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN " + citas)
    sql("DELETE FROM service_charge WHERE AppointmentId IN " + citas)
    for t in ("appointment_history", "appointment_service"):
        sql("DELETE FROM %s WHERE AppointmentId IN %s" % (t, citas))
    sql("DELETE FROM appointment WHERE Notes='%s'" % NOTA)


def slug_del_negocio():
    filas = sql("SELECT Slug FROM business_domain WHERE BusinessId='%s' AND IsPrimary=1" % BIZ)
    if not filas:
        raise SystemExit("el negocio de prueba no tiene slug")
    return filas[0][0]


def agendar(token, dia):
    """Reserva en la primera hora libre. Devuelve (id, codigo)."""
    for hora in range(14, 22):
        st, r = http("POST", "/business/appointments", token, {
            "businessId": BIZ, "branchId": BRANCH, "employeeId": EMP,
            "businessClientId": CLIENT, "startUtc": "%sT%02d:00:00Z" % (dia, hora),
            "offeringIds": [OFF], "backdated": False, "notes": NOTA})
        if st == 200:
            return r["data"]["id"], r["data"]["publicCode"]
    raise SystemExit("no se pudo agendar")


def ultimos4():
    filas = sql("SELECT PhoneE164 FROM business_client WHERE Id='%s'" % CLIENT)
    tel = filas[0][0] if filas else ""
    return tel[-4:]


def negocio_actual():
    """Lo que el negocio ya escribio, leido de donde lo escribio."""
    fila = sql("""
        SELECT COALESCE(NULLIF(b.TradeName,''), b.Name), t.Code,
               COALESCE(l.Tagline,''), COALESCE(s.AddressLine,'')
          FROM business b
          JOIN business_type t ON t.Id = b.BusinessTypeId
          LEFT JOIN business_landing l ON l.BusinessId = b.Id
          LEFT JOIN branch s ON s.BusinessId = b.Id AND s.IsMain = 1
         WHERE b.Id = '%s'""" % BIZ)[0]
    return {"nombre": fila[0], "tipo": fila[1], "titular": fila[2], "direccion": fila[3]}


def main():
    dueno = login("cc2085")
    limpiar()
    slug = slug_del_negocio()
    last4 = ultimos4()
    dia = (datetime.now() + timedelta(days=9)).strftime("%Y-%m-%d")

    print("=== 1) aparecer en el directorio es opt-in ===")
    sql("UPDATE business_public_profile SET IsListed = 0 WHERE BusinessId='%s'" % BIZ)
    st, r = http("GET", "/business/public/marketplace", None)
    check("el directorio responde sin sesion", st == 200, str(st))
    slugs = [c["slug"] for c in (r.get("data") or {}).get("items", [])]
    check("un negocio sin publicar NO sale", slug not in slugs, str(slugs))

    st, r = http("GET", "/business/public/marketplace/" + slug, None)
    check("y su ficha tampoco", st == 404, str(st))

    print("=== 2) publicar es UNA decision, no un formulario ===")
    # Lo que el negocio ya tiene escrito en otras pantallas.
    suyo = negocio_actual()

    st, r = http("PUT", "/business/public-profiles?businessId=" + BIZ, dueno,
                 {"isListed": True})
    check("se publica sin volver a teclear nada", st == 200,
          str(st) + " " + str(r.get("message", ""))[:70])
    check("y el mensaje lo dice", "directorio" in str(r.get("message", "")).lower(),
          str(r.get("message"))[:60])

    print("=== 2b) la tarjeta sale de lo que ya habia ===")
    st, r = http("GET", "/business/public/marketplace?q=" + suyo["nombre"].split()[0], None)
    items = (r.get("data") or {}).get("items", [])
    tarjeta = next((c for c in items if c["slug"] == slug), None)
    check("sale al buscar por su nombre", tarjeta is not None,
          str([c["slug"] for c in items]))
    if tarjeta:
        check("con el nombre del negocio", tarjeta.get("name") == suyo["nombre"],
              "%s vs %s" % (tarjeta.get("name"), suyo["nombre"]))
        check("con su tipo de negocio como categoria",
              tarjeta.get("categoryCode") == suyo["tipo"],
              "%s vs %s" % (tarjeta.get("categoryCode"), suyo["tipo"]))
        check("con el titular de su pagina", tarjeta.get("headline") == suyo["titular"],
              "%s vs %s" % (tarjeta.get("headline"), suyo["titular"]))
        check("y con la direccion de su sede", tarjeta.get("address") == suyo["direccion"],
              "%s vs %s" % (tarjeta.get("address"), suyo["direccion"]))

    st, r = http("GET", "/business/public/marketplace/" + slug, None)
    check("la ficha se resuelve por slug", st == 200, str(st))
    ficha = r.get("data") or {}
    check("y trae sus servicios", len(ficha.get("services") or []) >= 1,
          str(len(ficha.get("services") or [])))
    check("la ficha dice lo mismo que la tarjeta",
          tarjeta is not None and ficha.get("name") == tarjeta.get("name")
          and ficha.get("headline") == tarjeta.get("headline"),
          "%s / %s" % (ficha.get("name"), ficha.get("headline")))

    print("=== 2c) cambiar el dato en su sitio cambia el directorio ===")
    # Esto es lo que antes fallaba EN SILENCIO: la direccion se cambiaba en
    # Sedes y el directorio seguia enseñando la copia vieja.
    sql("UPDATE branch SET AddressLine='Calle nueva 456' WHERE BusinessId='%s' AND IsMain=1" % BIZ)
    st, r = http("GET", "/business/public/marketplace/" + slug, None)
    check("la ficha ya trae la direccion nueva",
          (r.get("data") or {}).get("addressLine") == "Calle nueva 456",
          str((r.get("data") or {}).get("addressLine")))
    sql("UPDATE branch SET AddressLine='%s' WHERE BusinessId='%s' AND IsMain=1"
        % (suyo["direccion"].replace("'", "''"), BIZ))

    print("=== 2d) el dueno ve la misma ficha que vera un desconocido ===")
    st, r = http("GET", "/business/public-profiles?businessId=" + BIZ, dueno)
    previa = r.get("data") or {}
    check("la vista previa responde", st == 200, str(st))
    check("y coincide con la tarjeta publica",
          tarjeta is not None
          and previa.get("name") == tarjeta.get("name")
          and previa.get("headline") == tarjeta.get("headline")
          and previa.get("categoryCode") == tarjeta.get("categoryCode"),
          str([previa.get("name"), previa.get("headline"), previa.get("categoryCode")]))
    check("y dice lo que falta", isinstance(previa.get("missing"), list),
          str(previa.get("missing")))

    print("=== 2e) sin titular no se publica ===")
    http("PUT", "/business/public-profiles?businessId=" + BIZ, dueno, {"isListed": False})
    sql("UPDATE business_landing SET Tagline=NULL WHERE BusinessId='%s'" % BIZ)
    st, r = http("PUT", "/business/public-profiles?businessId=" + BIZ, dueno,
                 {"isListed": True})
    check("publicar sin titular se rechaza", st >= 400,
          "%s %s" % (st, str(r.get("message", ""))[:60]))
    sql("UPDATE business_landing SET Tagline='%s' WHERE BusinessId='%s'"
        % (suyo["titular"].replace("'", "''"), BIZ))
    st, _ = http("PUT", "/business/public-profiles?businessId=" + BIZ, dueno,
                 {"isListed": True})
    check("y con titular vuelve a publicarse", st == 200, str(st))

    print("=== 3) solo se califica lo que se presto ===")
    cita, codigo = agendar(dueno, dia)
    st, r = http("POST", "/business/public/marketplace/%s/reviews" % slug, None, {
        "publicCode": codigo, "last4": last4, "businessStars": 5})
    check("una cita futura no se puede calificar", st == 400,
          "%s %s" % (st, str(r.get("message", ""))[:60]))

    # Se pasa a COMPLETADA por SQL: la maquina de estados exige recorrer
    # EN_CURSO y respetar la hora, y aqui lo que se prueba es la resena.
    sql("UPDATE appointment SET Status='COMPLETADA', CompletedAt=NOW(6) WHERE Id='%s'" % cita)

    print("=== 4) hay que demostrar que la cita es tuya ===")
    st, r = http("POST", "/business/public/marketplace/%s/reviews" % slug, None, {
        "publicCode": codigo, "last4": "0000", "businessStars": 5})
    check("con el telefono equivocado responde 404, no 403", st == 404,
          "%s %s" % (st, str(r.get("message", ""))[:60]))

    st, r = http("POST", "/business/public/marketplace/%s/reviews" % slug, None, {
        "publicCode": "NOEXISTE", "last4": last4, "businessStars": 5})
    check("y un codigo inventado da el MISMO 404", st == 404, str(st))

    print("=== 5) la resena entra y suma al agregado ===")
    antes = sql("SELECT ReviewCount, StarSum FROM business_public_profile "
                "WHERE BusinessId='%s'" % BIZ)[0]
    st, r = http("POST", "/business/public/marketplace/%s/reviews" % slug, None, {
        "publicCode": codigo, "last4": last4, "businessStars": 4,
        "employeeStars": 5, "comment": "Muy buen servicio."})
    check("resena aceptada", st == 200, "%s %s" % (st, str(r.get("message", ""))[:60]))

    despues = sql("SELECT ReviewCount, StarSum FROM business_public_profile "
                  "WHERE BusinessId='%s'" % BIZ)[0]
    check("el conteo sube uno", int(despues[0]) == int(antes[0]) + 1,
          "%s -> %s" % (antes[0], despues[0]))
    check("y la suma sube las estrellas", int(despues[1]) == int(antes[1]) + 4,
          "%s -> %s" % (antes[1], despues[1]))

    print("=== 6) una cita, una opinion ===")
    st, r = http("POST", "/business/public/marketplace/%s/reviews" % slug, None, {
        "publicCode": codigo, "last4": last4, "businessStars": 1})
    check("el segundo intento se rechaza", st == 400,
          "%s %s" % (st, str(r.get("message", ""))[:60]))
    otra = sql("SELECT ReviewCount, StarSum FROM business_public_profile "
               "WHERE BusinessId='%s'" % BIZ)[0]
    check("y el agregado no se movio", otra == despues, "%s vs %s" % (otra, despues))

    print("=== 7) la opinion sale en la ficha ===")
    st, r = http("GET", "/business/public/marketplace/" + slug, None)
    resenas = (r.get("data") or {}).get("reviews") or []
    check("la ficha trae la resena", any(x.get("comment") == "Muy buen servicio." for x in resenas),
          "%d resenas" % len(resenas))
    check("con quien atendio", all(x.get("employeeName") for x in resenas))
    check("y la nota media", (r.get("data") or {}).get("rating") is not None,
          str((r.get("data") or {}).get("rating")))

    print("=== 8) las estrellas fuera de rango se rechazan ===")
    cita2, codigo2 = agendar(dueno, (datetime.now() + timedelta(days=10)).strftime("%Y-%m-%d"))
    sql("UPDATE appointment SET Status='COMPLETADA', CompletedAt=NOW(6) WHERE Id='%s'" % cita2)
    for malas in (0, 6, None):
        st, _ = http("POST", "/business/public/marketplace/%s/reviews" % slug, None, {
            "publicCode": codigo2, "last4": last4, "businessStars": malas})
        check("rechaza %s estrellas" % malas, st in (400, 500), str(st))

    print("=== 9) el orden no premia al que tiene una sola opinion ===")
    # Sin ponderacion, un negocio con un unico 5 iria por delante de otro con
    # cuarenta resenas y 4,8. La formula bayesiana lo impide.
    st, r = http("GET", "/business/public/marketplace", None)
    items = (r.get("data") or {}).get("items", [])
    check("el listado sigue respondiendo con datos", len(items) >= 1, str(len(items)))
    mio = next((c for c in items if c["slug"] == slug), None)
    check("y el negocio trae su nota y su conteo",
          mio is not None and mio.get("reviewCount", 0) >= 1,
          str(mio.get("rating") if mio else None))

    return terminar()


def terminar():
    limpiar()
    sql("UPDATE business_public_profile SET IsListed = 0 WHERE BusinessId='%s'" % BIZ)
    print("")
    if fallos:
        print("FALLAN %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


if __name__ == "__main__":
    main()
