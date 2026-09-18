# -*- coding: utf-8 -*-
"""Comprueba la vinculacion del WhatsApp de un negocio.

Que se afirma:

  * sin conectar, el negocio lo dice, y el endpoint interno no entrega nada;
  * unas credenciales inventadas las RECHAZA Meta, no se guardan a ciegas;
  * el token se guarda CIFRADO — en la base no esta el valor en claro — y el
    envio lo recupera descifrado por el canal interno;
  * desconectar BORRA de verdad las columnas. Esto es lo que se escapaba: el
    `applyChanges` de la politica solo asigna lo que no es nulo, asi que por ahi
    desconectar habria dejado el token puesto mientras la pantalla decia que no;
  * el alta del webhook: la URL la dice el SERVIDOR (no la arma la pantalla), y
    cuando el alta automatica contra Meta no sale, la marca se queda NULA — que
    es lo que hace que al dueño se le sigan enseñando las instrucciones a mano.
    Aqui no sale nunca, porque las credenciales son inventadas: eso es justo el
    camino que hay que comprobar que no miente.

Uso:  python scripts/verificar-vinculacion-whatsapp.py
"""
import base64
import hashlib
import hmac
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

GW = "http://localhost:8080"
# El endpoint interno NO pasa por el gateway: lo llama events-service de
# servicio a servicio, y por eso el gateway lo rechaza con 401. Aqui se le
# habla directo al contenedor, igual que hace Feign.
BUSINESS = "http://localhost:8086/business"
BIZ = "b71117a2-b196-442a-b1cc-a581cb0d7171"
DUENO = ("cc2085", "Password123!")

TOKEN_FALSO = "EAAG-token-de-prueba-que-Meta-no-conoce"
APP_SECRET = "secreto-de-la-app-del-negocio"
VERIFY = "palabra-del-apreton-de-este-negocio"
PHONE_ID = "123456789012345"
WABA_ID = "987654321098765"

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


def http(method, path, token=None, body=None, base=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request((base or GW) + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    def leer(codigo, crudo):
        # El apreton de manos responde TEXTO PLANO (el reto de Meta), no JSON:
        # envolverlo haria que Meta lo rechazara.
        try:
            return codigo, json.loads(crudo or "{}")
        except ValueError:
            return codigo, {"raw": crudo}

    try:
        with urllib.request.urlopen(req) as r:
            return leer(r.status, r.read().decode())
    except urllib.error.HTTPError as e:
        return leer(e.code, e.read().decode())


def post_firmado(firma, cuerpo):
    """Una entrega como la manda Meta: el cuerpo EXACTO y su firma."""
    req = urllib.request.Request(GW + "/business/public/whatsapp/webhook",
                                 data=cuerpo.encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("X-Hub-Signature-256", firma)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def login(user, password):
    st, r = http("POST", "/auth/login", body={"usernameOrEmail": user, "password": password})
    if st != 200:
        raise SystemExit("login %s -> %s" % (user, st))
    return r["data"]["tokens"]["accessToken"]


def cifrar(claro):
    """El mismo AES-GCM que SecretBox, para poder dejar un token guardado.

    Necesita el paquete `cryptography` (pip install cryptography). Es una
    dependencia de ESTE guion, no del producto.
    """
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    llave = hashlib.sha256(env("SECRETS_KEY").encode()).digest()
    iv = os.urandom(12)
    ct = AESGCM(llave).encrypt(iv, claro.encode(), None)
    return base64.b64encode(iv + ct).decode()


def main():
    token = login(*DUENO)

    print("=== 1) sin conectar, se dice ===")
    sql("UPDATE saas_db.business_booking_policy SET WhatsappPhoneId=NULL, "
        "WhatsappWabaId=NULL, WhatsappWebhookAt=NULL, "
        "WhatsappAccessToken=NULL, WhatsappDisplayPhone=NULL, WhatsappVerifiedName=NULL, "
        "WhatsappVerifiedAt=NULL, WhatsappEnabled=0 WHERE BusinessId='%s'" % BIZ)

    st, r = http("GET", "/business/whatsapp", token)
    check("el estado se consulta", st == 200, str(st))
    check("dice que no esta conectado", st == 200 and r["data"]["conectado"] is False,
          str(r.get("data")))

    st, r = http("GET", "/internal/whatsapp-credentials?businessId=" + BIZ, base=BUSINESS)
    check("el canal interno no entrega credenciales",
          st == 200 and not (r or {}).get("phoneNumberId"), str(r)[:60])

    print("=== 2) credenciales inventadas: las rechaza Meta ===")
    st, r = http("POST", "/business/whatsapp", token,
                 {"phoneNumberId": PHONE_ID, "wabaId": WABA_ID, "accessToken": TOKEN_FALSO})
    check("no se guardan a ciegas", st >= 400, str(st) + " " + str(r.get("message"))[:70])

    fila = sql("SELECT WhatsappPhoneId FROM saas_db.business_booking_policy "
               "WHERE BusinessId='%s'" % BIZ)
    check("y la base sigue sin numero", not fila or fila[0][0] in ("NULL", "", None),
          str(fila))

    print("=== 3) con credenciales guardadas, el envio las recupera ===")
    guardado = cifrar(TOKEN_FALSO)
    sql("UPDATE saas_db.business_booking_policy SET WhatsappPhoneId='%s', "
        "WhatsappAccessToken='%s', WhatsappDisplayPhone='+57 300 111 2233', "
        "WhatsappVerifiedName='Barberia CC', WhatsappVerifiedAt=NOW(6), "
        "WhatsappEnabled=1 WHERE BusinessId='%s'" % (PHONE_ID, guardado, BIZ))

    st, r = http("GET", "/business/whatsapp", token)
    d = r.get("data", {})
    check("el estado dice conectado", st == 200 and d.get("conectado") is True, str(st))
    check("con el numero legible", d.get("displayPhone") == "+57 300 111 2233",
          str(d.get("displayPhone")))
    check("el token NO sale hacia la pantalla", "accessToken" not in json.dumps(d),
          str(d)[:70])

    # La URL del webhook la dice el servidor: si la armara tambien la pantalla,
    # un dia dirian cosas distintas y el dueño pegaria en Meta una que no es.
    check("el estado trae la URL del webhook, y la pone el servidor",
          str(d.get("urlWebhook", "")).endswith("/business/public/whatsapp/webhook"),
          str(d.get("urlWebhook")))
    check("y sin alta automatica la marca va vacia", d.get("webhookAt") is None,
          str(d.get("webhookAt")))

    # Lo que hay guardado no puede ser el token en claro.
    fila = sql("SELECT WhatsappAccessToken FROM saas_db.business_booking_policy "
               "WHERE BusinessId='%s'" % BIZ)
    check("en la base esta cifrado, no en claro",
          fila and TOKEN_FALSO not in fila[0][0], fila[0][0][:30] if fila else "-")

    # El canal interno SI lo entrega descifrado: es quien envia.
    st, r = http("GET", "/internal/whatsapp-credentials?businessId=" + BIZ, base=BUSINESS)
    check("el canal interno lo devuelve descifrado",
          st == 200 and (r or {}).get("accessToken") == TOKEN_FALSO,
          str((r or {}).get("accessToken"))[:30])
    check("y con su numero", (r or {}).get("phoneNumberId") == PHONE_ID,
          str((r or {}).get("phoneNumberId")))

    print("=== 4) el webhook usa el secreto DE ESE NEGOCIO ===")
    # Con credenciales pegadas cada negocio tiene SU app de Meta, y Meta firma
    # con el secreto de esa app. Comprobando contra un unico secreto de
    # plataforma, ningun negocio conectado recibiria una sola cita.
    sql("UPDATE saas_db.business_booking_policy SET WhatsappAppSecret='%s', "
        "WhatsappVerifyToken='%s' WHERE BusinessId='%s'"
        % (cifrar(APP_SECRET), VERIFY, BIZ))

    st, r = http("GET", "/business/public/whatsapp/webhook?hub.mode=subscribe"
                        "&hub.verify_token=%s&hub.challenge=hola" % VERIFY)
    check("el apreton de manos acepta la palabra del negocio",
          st == 200 and r.get("raw") == "hola", "%s %s" % (st, r))

    st, r = http("GET", "/business/public/whatsapp/webhook?hub.mode=subscribe"
                        "&hub.verify_token=palabra-inventada&hub.challenge=hola")
    check("y rechaza una inventada", st >= 400, str(st))

    cuerpo = json.dumps({"entry": [{"changes": [{"value": {
        "metadata": {"phone_number_id": PHONE_ID}, "messages": []}}]}]})

    def firmar(secreto):
        return "sha256=" + hmac.new(secreto.encode(), cuerpo.encode(), hashlib.sha256).hexdigest()

    st, _ = post_firmado(firmar(APP_SECRET), cuerpo)
    check("una entrega firmada con el secreto del negocio entra", st == 200, str(st))

    st, _ = post_firmado(firmar("otro-secreto-cualquiera"), cuerpo)
    check("y con otro secreto se rechaza", st == 403, str(st))

    print("=== 4b) reconectar vuelve a exigir el alta del webhook ===")
    # Si la marca sobreviviera a un cambio de credenciales, la pantalla diria
    # "no tienes que hacer nada" con un webhook que ya no existe.
    sql("UPDATE saas_db.business_booking_policy SET WhatsappWebhookAt=NOW(6), "
        "WhatsappWabaId='%s' WHERE BusinessId='%s'" % (WABA_ID, BIZ))
    st, r = http("GET", "/business/whatsapp", token)
    check("con marca puesta, el estado la dice",
          (r.get("data") or {}).get("webhookAt") is not None,
          str((r.get("data") or {}).get("webhookAt")))

    st, _ = http("POST", "/business/whatsapp", token,
                 {"phoneNumberId": PHONE_ID, "wabaId": WABA_ID, "accessToken": TOKEN_FALSO})
    # Las credenciales son falsas, asi que Meta las rechaza y no se toca nada.
    fila = sql("SELECT IFNULL(WhatsappWabaId,'-') FROM saas_db.business_booking_policy "
               "WHERE BusinessId='%s'" % BIZ)[0]
    check("un intento rechazado no borra lo que ya servia", fila[0] == WABA_ID, fila[0])

    print("=== 5) desconectar BORRA de verdad ===")
    st, r = http("DELETE", "/business/whatsapp", token)
    check("desconecta", st == 200, str(st))

    fila = sql("SELECT IFNULL(WhatsappPhoneId,'-'), IFNULL(WhatsappAccessToken,'-'), "
               "IFNULL(WhatsappDisplayPhone,'-'), WhatsappEnabled, "
               "IFNULL(WhatsappWabaId,'-'), IFNULL(WhatsappWebhookAt,'-') "
               "FROM saas_db.business_booking_policy WHERE BusinessId='%s'" % BIZ)[0]
    # Con `applyChanges` esto habria fallado: solo asigna lo que no es nulo, asi
    # que el token se habria quedado puesto.
    check("el numero se borro", fila[0] == "-", fila[0])
    check("el token se borro", fila[1] == "-", fila[1][:20])
    check("el numero legible se borro", fila[2] == "-", fila[2])
    check("y el canal queda apagado", fila[3] == "0", fila[3])
    check("la cuenta de WhatsApp se borro", fila[4] == "-", fila[4])
    check("y el alta del webhook deja de contar", fila[5] == "-", fila[5])

    st, r = http("GET", "/internal/whatsapp-credentials?businessId=" + BIZ, base=BUSINESS)
    check("el envio ya no recibe nada", st == 200 and not (r or {}).get("accessToken"),
          str(r)[:60])

    print()
    if fallos:
        print("FALLARON %d:" % len(fallos))
        for f in fallos:
            print("  - " + f)
        sys.exit(1)
    print("TODO OK")


main()
