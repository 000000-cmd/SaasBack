# -*- coding: utf-8 -*-
"""Deja el negocio de demostración con una agenda que se pueda enseñar.

Por que existe: el negocio sembrado tiene UNA profesional y citas sueltas. Con
eso no se ve nada de lo que hace el calendario — ni columnas por profesional,
ni solapamiento, ni una cita creciendo pasada de su hora. Este guion monta un
dia realista para revisarlo a ojo o grabarlo.

Que deja:
  * tres profesionales en la sede principal (crea los que falten);
  * un dia con citas encadenadas, dos pares que se pisan y un trio simultaneo;
  * una cita EN CURSO que ya se paso de su hora (el bloque que crece);
  * una pendiente de confirmar, una terminada y una que el cliente cancelo;
  * un bloqueo de agenda (almuerzo) que se pisa con una cita;
  * el negocio publicado en el directorio, con opiniones reales sobre citas
    terminadas (que es la unica forma en que se pueden dejar).

Es repetible: borra lo suyo antes de sembrar. Todo lo que crea lleva la nota
DEMO para poder distinguirlo y limpiarlo.

Uso:  python scripts/sembrar-demo-agenda.py
      python scripts/sembrar-demo-agenda.py --limpiar   (solo borra)
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

GW = "http://localhost:8080"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
DUENO = ("cc2085", "Password123!")
# El nombre de una persona NO lo puede tocar el dueno: por diseno lo completa
# el propio empleado desde el APK, o un admin. Por eso hace falta este otro.
ADMIN = ("admin", "Admin123!")
NOTA = "DEMO"
CODIGO_DEMO = "demo-agenda"


def dbpw():
    for line in open(r"C:\SaasBack\.env", encoding="utf-8"):
        if line.startswith("MYSQL_ROOT_PASSWORD="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("sin password")


def sql(q, db="saas_db"):
    # --default-character-set=utf8mb4 NO es opcional: sin el, el cliente
    # negocia latin1 y un nombre con tilde se guarda con los bytes cambiados.
    # Se ve en pantalla como "BenÃ­tez" y parece un fallo del front.
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


def login(user, password):
    st, r = http("POST", "/auth/login", body={"usernameOrEmail": user, "password": password})
    if st != 200:
        raise SystemExit("login %s -> %s %s" % (user, st, json.dumps(r)[:200]))
    return r["data"]["tokens"]["accessToken"]


def limpiar():
    citas = "(SELECT Id FROM appointment WHERE Notes LIKE '%s%%')" % NOTA
    sql("DELETE FROM appointment_review WHERE AppointmentId IN " + citas)
    sql("DELETE FROM appointment_notification WHERE AppointmentId IN " + citas)
    sql("DELETE FROM service_charge WHERE AppointmentId IN " + citas)
    for t in ("appointment_history", "appointment_service"):
        sql("DELETE FROM %s WHERE AppointmentId IN %s" % (t, citas))
    sql("DELETE FROM appointment WHERE Notes LIKE '%s%%'" % NOTA)
    sql("DELETE FROM agenda_exception WHERE Reason LIKE '%s%%'" % NOTA)
    sql("DELETE FROM business_client WHERE Notes = '%s'" % CODIGO_DEMO)
    # El agregado se recalcula desde las resenas que QUEDAN: dejarlo como
    # estaba contaria opiniones que se acaban de borrar.
    sql("UPDATE business_public_profile p SET "
        "p.ReviewCount = (SELECT COUNT(*) FROM appointment_review r "
        "                  WHERE r.BusinessId = p.BusinessId AND r.Visible = 1), "
        "p.StarSum = COALESCE((SELECT SUM(r.BusinessStars) FROM appointment_review r "
        "                       WHERE r.BusinessId = p.BusinessId AND r.Visible = 1), 0) "
        "WHERE p.BusinessId = '%s'" % BIZ)
    print("limpio")


# ---------------------------------------------------------------- profesionales

_admin = []


def admin_token():
    if not _admin:
        _admin.append(login(*ADMIN))
    return _admin[0]


def profesionales(token):
    """Los de la sede, creando los que falten hasta llegar a tres.

    Tres y no dos: con dos, un solapamiento a tres bandas —que es donde el
    reparto de columnas se nota— no se puede montar.

    Se cuentan contra la BASE y no contra `/employees/detailed`, que lee el
    read model de Elasticsearch. Contra ES, si la proyeccion aun no habia
    llegado, el guion creaba empleados de mas en cada pasada — y la agenda
    acababa con cinco columnas y dos nombres repetidos.
    """
    filas = sql("SELECT e.Id, CONCAT(COALESCE(tp.FirstName,''),' ',COALESCE(tp.FirstLastName,'')) "
                "FROM saas_db.employee e JOIN personas.third_party tp ON tp.Id = e.ThirdPartyId "
                "WHERE e.BranchId='%s' AND e.Visible = 1 ORDER BY e.CreatedDate" % BRANCH)
    actuales = [(f[0], f[1].strip()) for f in filas]

    nombres = ["Ana Rivas", "Mateo Benítez", "Sara Klein"]
    while len(actuales) < 3:
        sufijo = uuid.uuid4().hex[:6]
        st, r = http("POST", "/business/employees/provision", token, {
            "branchId": BRANCH,
            "username": "demo" + sufijo,
            "email": "demo" + sufijo + "@moda.local",
            "password": "Password123!",
        })
        if st != 200:
            raise SystemExit("provision -> %s %s" % (st, json.dumps(r)[:200]))
        emp = r["data"]["employeeId"]
        # El alta minima deja la persona sin nombre; el APK lo completa en su
        # primer ingreso. Aqui se pone por API y NO por SQL, y eso importa:
        #
        #   * por SQL, los acentos se guardan con la codificacion del cliente y
        #     acaban en "BenÃ­tez";
        #   * y aunque se guarden bien, el documento de Elasticsearch lleva una
        #     version derivada de la fila. Un UPDATE directo no la mueve, asi
        #     que el reindex posterior llega "viejo" y ES lo DESCARTA — el
        #     nombre queda bien en la base y mal en la pantalla.
        #
        # Por el endpoint pasa por el outbox y la proyeccion se entera.
        nombre = nombres[len(actuales)]
        partes = nombre.split(" ")
        tp = sql("SELECT ThirdPartyId FROM employee WHERE Id='%s'" % emp)[0][0]
        st, r2 = http("PUT", "/thirdparty/third-parties/" + tp, admin_token(),
                      {"firstName": partes[0], "firstLastName": partes[-1]})
        # Se revienta a proposito: con el token del dueno esto daba 403 y solo
        # se avisaba por pantalla, asi que el demo se quedaba con columnas sin
        # nombre y parecia un fallo del calendario.
        if st != 200:
            raise SystemExit("no se pudo nombrar a %s: %s %s" % (nombre, st, json.dumps(r2)[:200]))
        actuales.append((emp, nombre))
        print("  + profesional", nombre)
    return [e[0] for e in actuales]


# El catalogo del demo: categorias y servicios con descripcion. Sin esto la
# pantalla de reserva sale con UN servicio pelado y no se parece en nada al
# diseno, que ensena tarjetas con descripcion, duracion y categoria.
CATALOGO = [
    ("Cortes y estilismo", [
        ("Corte degradado y texturizado",
         "Desvanecido a maquina y tijera, lavado y secado con acabado mate.", 35, 40000),
        ("Corte clasico de tijera",
         "Corte a tijera con peinado incluido, para quien no quiere maquina.", 30, 32000),
    ]),
    ("Barba y afeitado", [
        ("Perfilado de barba con toalla caliente",
         "Toalla caliente, aceites esenciales, navaja y balsamo calmante.", 25, 22000),
    ]),
    ("Tratamientos", [
        ("Lavado y masaje capilar",
         "Masaje craneal con tonico mentolado para soltar tension.", 20, 18000),
        ("Tratamiento hidratante",
         "Mascarilla de colageno con sellado termico para cabello seco.", 40, 35000),
    ]),
]


def catalogo(token):
    """Deja el catalogo del demo con categorias y servicios descritos.

    Solo crea lo que falte, por NOMBRE: correrlo dos veces no duplica nada.
    """
    st, r = http("GET", "/business/offering-categories?businessId=" + BIZ, token)
    porNombre = {c["name"]: c["id"] for c in (r.get("data") or [])}

    st, r = http("GET", "/business/offerings?businessId=" + BIZ, token)
    hay = {o["name"] for o in (r.get("data") or [])}

    for nombreCat, items in CATALOGO:
        catId = porNombre.get(nombreCat)
        if not catId:
            st, r = http("POST", "/business/offering-categories", token,
                         {"businessId": BIZ, "name": nombreCat, "displayOrder": 0})
            if st != 200:
                raise SystemExit("categoria %s -> %s %s" % (nombreCat, st, json.dumps(r)[:200]))
            catId = r["data"]["id"]
            print("  + categoria", nombreCat)

        for nombre, desc, mins, precio in items:
            if nombre in hay:
                continue
            st, r = http("POST", "/business/offerings", token, {
                "businessId": BIZ, "categoryId": catId, "name": nombre,
                "description": desc, "durationMinutes": mins, "price": precio,
                "isActive": True,
            })
            if st != 200:
                raise SystemExit("servicio %s -> %s %s" % (nombre, st, json.dumps(r)[:200]))
            print("  + servicio", nombre)


def servicios(token):
    st, r = http("GET", "/business/offerings?businessId=" + BIZ, token)
    activos = [o for o in (r.get("data") or []) if o.get("isActive")]
    if not activos:
        raise SystemExit("el negocio no tiene servicios activos")
    return activos


def cliente(token, nombre, telefono):
    st, r = http("POST", "/business/business-clients", token, {
        "businessId": BIZ, "displayName": nombre, "phone": telefono,
        "notes": CODIGO_DEMO})
    if st != 200:
        raise SystemExit("cliente -> %s %s" % (st, json.dumps(r)[:200]))
    return r["data"]["id"]


# ---------------------------------------------------------------------- agenda

def hoy_utc(hora, minuto=0):
    """Un instante de HOY a la hora local de Bogota (UTC-5), en UTC."""
    ahora = datetime.now(timezone.utc)
    return ahora.replace(hour=(hora + 5) % 24, minute=minuto, second=0, microsecond=0)


def agendar(token, emp, cli, offs, inicio, nota):
    st, r = http("POST", "/business/appointments", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": emp,
        "businessClientId": cli, "startUtc": inicio.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "offeringIds": offs, "backdated": False, "notes": nota})
    if st != 200:
        print("   (no se pudo agendar %s: %s)" % (nota, str(r.get("message"))[:70]))
        return None
    return r["data"]["id"]


def mover(cita_id, estado, motivo=None):
    """Se mueve por SQL a proposito: la maquina de estados exige pasar por
    EN_CURSO y respetar la hora, y aqui hace falta montar el ESTADO FINAL de
    una escena, no recorrerla."""
    extra = ""
    if estado == "EN_CURSO":
        extra = ", StartedAt = UTC_TIMESTAMP()"
    if estado == "COMPLETADA":
        extra = ", CompletedAt = UTC_TIMESTAMP()"
    if motivo:
        extra += ", CancelReason = '%s', CancelledAt = UTC_TIMESTAMP()" % motivo
    sql("UPDATE appointment SET Status='%s'%s WHERE Id='%s'" % (estado, extra, cita_id))


def desplazar(cita_id, inicio_h, inicio_m, dur_min):
    """Coloca la cita a una hora local concreta de hoy, sin pasar por el motor
    de disponibilidad — que rechazaria a proposito lo que se quiere montar:
    citas que se pisan."""
    sql("UPDATE appointment SET "
        "StartUtc = TIMESTAMP(UTC_DATE(), '%02d:%02d:00'), "
        "EndUtc = TIMESTAMP(UTC_DATE(), '%02d:%02d:00') + INTERVAL %d MINUTE, "
        "LocalDate = CURDATE() WHERE Id='%s'"
        % ((inicio_h + 5) % 24, inicio_m, (inicio_h + 5) % 24, inicio_m, dur_min, cita_id))


def bloqueo(token, emp, desde_h, hasta_h, motivo):
    st, r = http("POST", "/business/agenda-exceptions", token, {
        "businessId": BIZ, "branchId": BRANCH, "employeeId": emp,
        "startUtc": hoy_utc(desde_h).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "endUtc": hoy_utc(hasta_h).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "kind": "BLOCK", "reason": motivo})
    if st != 200:
        print("   (no se pudo bloquear: %s)" % str(r.get("message"))[:70])


def publicar_y_calificar(token, emps, clientes, offs):
    """Deja el negocio en el directorio y con opiniones.

    Las resenas se dejan por el endpoint PUBLICO, no por SQL: es el unico
    camino que comprueba que la cita esta COMPLETADA y que quien opina la
    tenia. Sembrarlas por SQL dejaria el agregado descuadrado y probaria justo
    lo que no hay que probar.
    """
    st, _ = http("PUT", "/business/public-profiles?businessId=" + BIZ, token, {
        "headline": "Cortes clásicos y barba, sin esperas",
        "description": "Barbería de barrio con doce años cortando. "
                       "Tres profesionales, reserva en línea y café de bienvenida.",
        "categoryCode": "BARBERSHOP",
        "addressLine": "Calle 10 #43-25, El Poblado",
        "isListed": True,
    })
    if st != 200:
        print("   (no se pudo publicar la ficha: %s)" % st)
        return

    slug = sql("SELECT Slug FROM business_domain WHERE BusinessId='%s' AND IsPrimary=1" % BIZ)
    if not slug:
        return
    slug = slug[0][0]

    # Citas de dias pasados, ya terminadas, sobre las que SI se puede opinar.
    opiniones = [(5, 5, "Puntualísimos y el corte quedó justo como pedí."),
                 (4, 5, "Muy buen ambiente. Esperé cinco minutos, nada grave."),
                 (5, 4, "Llevo tres años yendo. Nunca me han fallado.")]

    dejadas = 0
    for i, (negocio_estrellas, prof_estrellas, texto) in enumerate(opiniones):
        futuro = datetime.now(timezone.utc) + timedelta(days=40 + i)
        futuro = futuro.replace(hour=15, minute=0, second=0, microsecond=0)
        cita = agendar(token, emps[i % len(emps)], clientes[i], [offs[0]["id"]],
                       futuro, "%s resena %d" % (NOTA, i))
        if not cita:
            continue
        # Se lleva al pasado y a COMPLETADA: solo asi se puede calificar.
        sql("UPDATE appointment SET Status='COMPLETADA', "
            "StartUtc = UTC_TIMESTAMP() - INTERVAL %d DAY, "
            "EndUtc = UTC_TIMESTAMP() - INTERVAL %d DAY + INTERVAL 30 MINUTE, "
            "LocalDate = CURDATE() - INTERVAL %d DAY, CompletedAt = UTC_TIMESTAMP() "
            "WHERE Id='%s'" % (i + 3, i + 3, i + 3, cita))

        codigo = sql("SELECT PublicCode FROM appointment WHERE Id='%s'" % cita)[0][0]
        tel = sql("SELECT PhoneE164 FROM business_client WHERE Id='%s'" % clientes[i])
        last4 = tel[0][0][-4:] if tel and tel[0][0] else ""

        st, r = http("POST", "/business/public/marketplace/%s/reviews" % slug, None, {
            "publicCode": codigo, "last4": last4,
            "businessStars": negocio_estrellas, "employeeStars": prof_estrellas,
            "comment": texto})
        if st == 200:
            dejadas += 1
        else:
            print("   (resena %d no entró: %s %s)" % (i, st, str(r.get("message"))[:60]))

    print("  publicado en el directorio con %d opiniones (/directorio/%s)" % (dejadas, slug))


def main():
    if "--limpiar" in sys.argv:
        return limpiar()

    token = login(*DUENO)
    limpiar()

    catalogo(token)
    emps = profesionales(token)
    offs = servicios(token)
    corto = [offs[0]["id"]]
    largo = [o["id"] for o in offs[:2]] if len(offs) > 1 else corto

    clientes = [
        cliente(token, "Laura Gómez", "3001112233"),
        cliente(token, "Mauricio Echeverri", "3002223344"),
        cliente(token, "Camila Restrepo", "3003334455"),
        cliente(token, "Jorge Tamayo", "3004445566"),
        cliente(token, "Valeria Silva", "3005556677"),
        cliente(token, "Andrés Lara", "3006667788"),
    ]

    # Cada fila: (profesional, cliente, servicios, hora, minuto, duracion, estado)
    #
    # La escena esta pensada para que se vea CADA cosa del calendario:
    #   09:00-10:00 y 10:00-11:00 en la misma columna → encadenadas, ancho entero
    #   11:00-12:00 y 11:30-12:30 en la misma columna → se pisan, mitad y mitad
    #   13:00 tres a la vez en tres columnas          → un tercio cada una
    #   la de las 14:00 EN CURSO con 30 min           → creciendo si ya pasaron
    escena = [
        (0, 0, corto,  9,  0, 60, "COMPLETADA"),
        (0, 1, corto, 10,  0, 60, "COMPLETADA"),
        (0, 2, largo, 11,  0, 60, "CONFIRMADA"),
        (0, 3, corto, 11, 30, 60, "CONFIRMADA"),
        (0, 4, corto, 14,  0, 30, "EN_CURSO"),
        (0, 5, corto, 15,  0, 45, "PENDIENTE_CONFIRMACION"),
        (1, 1, corto,  9, 30, 45, "COMPLETADA"),
        (1, 2, largo, 13,  0, 60, "CONFIRMADA"),
        (1, 3, corto, 16,  0, 30, "CONFIRMADA"),
        (1, 4, corto, 17,  0, 30, "CANCELADA_CLIENTE"),
        (2, 0, corto, 13,  0, 45, "CONFIRMADA"),
        (2, 5, corto, 13, 15, 45, "CONFIRMADA"),
        (2, 3, corto, 10,  0, 90, "CONFIRMADA"),
    ]

    creadas = 0
    for i, (e, c, offs_i, h, m, dur, estado) in enumerate(escena):
        # Se agenda en un dia futuro libre y despues se desplaza a hoy: el motor
        # de disponibilidad rechaza lo que se pisa, que es justo lo que aqui se
        # quiere montar.
        futuro = datetime.now(timezone.utc) + timedelta(days=20 + i)
        futuro = futuro.replace(hour=14, minute=0, second=0, microsecond=0)
        cita = agendar(token, emps[e], clientes[c], offs_i, futuro, "%s %d" % (NOTA, i))
        if not cita:
            continue
        desplazar(cita, h, m, dur)
        if estado != "CONFIRMADA":
            mover(cita, estado, "Le surgió un imprevisto" if "CANCELADA" in estado else None)
        creadas += 1

    # La cita en curso se pasa de su hora: se le pone el fin ANTES de ahora para
    # que el bloque crezca. Es el caso que mas cuesta ver y el que mas importa.
    sql("UPDATE appointment SET StartUtc = UTC_TIMESTAMP() - INTERVAL 50 MINUTE, "
        "EndUtc = UTC_TIMESTAMP() - INTERVAL 20 MINUTE, LocalDate = CURDATE() "
        "WHERE Status='EN_CURSO' AND Notes LIKE '%s%%'" % NOTA)

    bloqueo(token, emps[1], 12, 13, "%s Almuerzo" % NOTA)

    publicar_y_calificar(token, emps, clientes, offs)

    print("sembradas %d citas para hoy, %d profesionales" % (creadas, len(emps)))
    print("entra como %s / %s en http://localhost:4200/tenant/agenda" % DUENO)


if __name__ == "__main__":
    main()
