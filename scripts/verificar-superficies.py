# -*- coding: utf-8 -*-
"""Comprueba quien puede entrar POR DONDE, y que lo decide el servidor.

Hay tres puertas y no dan al mismo sitio:

  * ADMIN    (:4201) — solo administradores de la plataforma;
  * PRODUCT  (:4200) — los duenos de negocio, y NO los empleados;
  * sin superficie   — el APK y las llamadas internas.

Lo que se afirma, y por que importa:

  * que el empleado NO recibe tokens al intentar entrar por la web. Esto
    se comprobaba SOLO en la pantalla de login: el servidor firmaba los
    tokens, el navegador los tiraba, y quien llamara a la API a mano se
    quedaba con una sesion valida y un panel entero devolviendole 403 —
    que es justo el sintoma de "me avisa de que no tengo permisos y aun
    asi me deja entrar";
  * que el MISMO empleado si entra sin superficie, porque el APK es su
    sitio y no manda ninguna;
  * que al empleado se le dice el motivo, y al administrador NO: que una
    cuenta sea de empleado no es secreto, pero confirmar que una cuenta
    es de administrador le regala medio trabajo a quien lo prueba;
  * que un dueno que ademas es empleado sigue entrando por la web.

Uso:  python scripts/verificar-superficies.py
"""
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request

GW = "http://localhost:8080"
BRANCH = "349586dd-f8f4-46c0-806a-ca5ad3fb7512"
DUENO = ("cc2085", "Password123!")
ADMIN = ("admin", "Admin123!")
CLAVE = "Password123!"

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
                        "-p" + env("MYSQL_ROOT_PASSWORD"), "-D", "saas_db", "-N", "-e", q],
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
        crudo = e.read().decode()
        try:
            return e.code, json.loads(crudo)
        except ValueError:
            return e.code, {"raw": crudo[:200]}


def entrar(usuario, clave, superficie=None):
    """Un intento de login. Devuelve (codigo, mensaje, si hubo tokens)."""
    cuerpo = {"usernameOrEmail": usuario, "password": clave}
    if superficie:
        cuerpo["surface"] = superficie
    st, r = http("POST", "/auth/login", body=cuerpo)
    hay = bool((r.get("data") or {}).get("tokens", {}).get("accessToken"))
    return st, str(r.get("message") or ""), hay


def main():
    st, r = http("POST", "/auth/login", body={"usernameOrEmail": DUENO[0], "password": DUENO[1]})
    if st != 200:
        raise SystemExit("no se pudo entrar como dueno: %s" % st)
    tok = r["data"]["tokens"]["accessToken"]

    # Un empleado nuevo para esta prueba: los que ya existen vienen de otras
    # suites y su clave no se conoce.
    emp = "supq%d" % int(time.time() % 1000000)
    st, r = http("POST", "/business/employees/provision", tok,
                 {"branchId": BRANCH, "username": emp,
                  "email": emp + "@e2e.local", "password": CLAVE})
    if st != 200:
        raise SystemExit("no se pudo crear el empleado de prueba: %s %s" % (st, r))

    try:
        print("=== 1) el empleado NO entra por la web ===")
        st, msg, hay = entrar(emp, CLAVE, "PRODUCT")
        check("el login se rechaza", st == 403, "%s %s" % (st, msg[:60]))
        # Lo importante no es el codigo, es que NO se firmen tokens: antes se
        # firmaban y era el navegador quien decidia tirarlos a la basura.
        check("y NO se emiten tokens", not hay, "hubo tokens" if hay else "")
        check("se le dice por que", "app" in msg.lower(), msg[:60])

        print("=== 2) el MISMO empleado si entra por el APK ===")
        st, msg, hay = entrar(emp, CLAVE)
        check("sin superficie entra", st == 200 and hay, "%s %s" % (st, msg[:50]))

        print("=== 3) al administrador no se le delata ===")
        st, msg, hay = entrar(ADMIN[0], ADMIN[1], "PRODUCT")
        check("no entra por la puerta del producto", st >= 400 and not hay, str(st))
        check("y el mensaje NO dice que sea administrador",
              "admin" not in msg.lower() and "administra" not in msg.lower(), msg[:60])

        st, msg, hay = entrar(ADMIN[0], ADMIN[1], "ADMIN")
        check("por la suya si entra", st == 200 and hay, "%s %s" % (st, msg[:50]))

        print("=== 4) el dueno entra por la web, y no por la de admin ===")
        st, msg, hay = entrar(DUENO[0], DUENO[1], "PRODUCT")
        check("el dueno entra", st == 200 and hay, str(st))
        st, msg, hay = entrar(DUENO[0], DUENO[1], "ADMIN")
        check("y no se cuela en la de administracion", st >= 400 and not hay, str(st))

        print("=== 5) quien es empleado Y dueno entra por la web ===")
        # Hay quien atiende en su propio local. Negarle su panel por tener
        # tambien ficha de empleado seria absurdo.
        sql("INSERT IGNORE INTO saas_db.user_role "
            "(Id, UserId, RoleId, Enabled, Visible, AuditDate, CreatedDate) "
            "SELECT UUID(), u.Id, '11111111-0000-0000-0000-000000000004', 1, 1, NOW(6), NOW(6) "
            "FROM saas_db.app_user u WHERE u.Username='%s'" % emp)
        st, msg, hay = entrar(emp, CLAVE, "PRODUCT")
        check("con los dos papeles, entra", st == 200 and hay, "%s %s" % (st, msg[:50]))

    finally:
        # Limpieza: el empleado de prueba no se queda en el equipo del negocio.
        # Hay que soltar antes todo lo que cuelga de la cuenta (las sesiones y
        # el aparato vinculado), o la clave ajena no deja borrarla.
        for t in ("refresh_token", "user_device_link", "user_role"):
            sql("DELETE x FROM saas_db.%s x JOIN saas_db.app_user u ON u.Id=x.UserId "
                "WHERE u.Username='%s'" % (t, emp))
        sql("DELETE e FROM saas_db.employee e JOIN personas.third_party t ON t.Id=e.ThirdPartyId "
            "JOIN saas_db.app_user u ON u.Id=t.UserId WHERE u.Username='%s'" % emp)
        sql("DELETE t FROM personas.third_party t JOIN saas_db.app_user u ON u.Id=t.UserId "
            "WHERE u.Username='%s'" % emp)
        sql("DELETE FROM saas_db.app_user WHERE Username='%s'" % emp)

    print()
    if fallos:
        print("FALLARON %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


main()
