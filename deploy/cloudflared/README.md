# Publicar el backend en `back.sylvanor.lat`

> **Por qué NO `saas.back.sylvanor.lat`.** El certificado gratuito de Cloudflare
> cubre `sylvanor.lat` y `*.sylvanor.lat`, y un comodín TLS abarca exactamente
> UNA etiqueta. Con tres niveles el borde no tenía certificado que presentar y
> cerraba la conexión con `handshake_failure`: la app ni siquiera llegaba a
> mandar la petición. Un tercer nivel exige Advanced Certificate Manager, que se
> paga. De ahí que el host tenga un solo nivel.


El túnel de Cloudflare deja el gateway accesible desde internet sin abrir
puertos en el router ni tener IP pública. La app del colaborador apunta ahí.

Ya está preparado todo lo que se puede preparar desde el repo. **Faltan cuatro
comandos que tienes que correr tú**, porque autentican contra tu cuenta de
Cloudflare y crean registros DNS en tu zona.

---

## 1. Autenticar (una sola vez)

```bash
cloudflared tunnel login
```

Abre el navegador. Elige la zona **`sylvanor.lat`**. Al terminar deja un
`cert.pem` en `C:\Users\jeiss\.cloudflared\`.

## 2. Crear el túnel (una sola vez)

```bash
cloudflared tunnel create saasback
```

Imprime un **UUID** y escribe `C:\Users\jeiss\.cloudflared\<UUID>.json`. Copia
ese UUID en `credentials-file` dentro de [`config.yml`](config.yml), donde ahora
pone el UUID que imprimió.

## 3. Apuntar el dominio al túnel (una sola vez)

```bash
cloudflared tunnel route dns saasback back.sylvanor.lat
```

Crea el CNAME en Cloudflare. Si el registro ya existe, añade `--overwrite-dns`.

## 4. Levantarlo

```bash
cloudflared tunnel --config deploy/cloudflared/config.yml run
```

Para que sobreviva a reinicios, como servicio de Windows (PowerShell **como
administrador**):

```bash
cloudflared --config C:\SaasBack\deploy\cloudflared\config.yml service install
```

---

## Comprobar que quedó bien

Con el backend arriba (`docker compose up -d`) y el túnel corriendo:

```bash
curl -s https://back.sylvanor.lat/api/info
```

Y la ruta pública de una factura, que es la que se manda por SMS:

```bash
curl -s -o factura.pdf -w "%{http_code} %{size_download}\n" https://back.sylvanor.lat/finance/public/invoices/<id-del-servicio>
```

---

## Lo que ya está hecho en el repo

| Qué | Dónde |
|---|---|
| El APK apunta al dominio por defecto | `SaasApk/lib/core/config/env.dart` |
| Cleartext permitido solo en local | `SaasApk/android/app/src/main/res/xml/network_security_config.xml` |
| El dominio en los orígenes CORS | `saas-config-repo/gateway-service.properties` |
| El enlace de las facturas usa el dominio | `saas-config-repo/finance-service.properties` |
| El límite de peticiones cuenta la IP real | `gateway-service/.../KeyResolverConfig.java` |
| Reglas de entrada del túnel | `deploy/cloudflared/config.yml` |

### Por qué el límite de peticiones tuvo que cambiar

Detrás del túnel, la dirección remota que ve el gateway es siempre la del
conector. Sin leer la cabecera correcta, **todos los usuarios compartirían un
solo cubo** y el primero en pasarse dejaría a los demás sin poder entrar.

Se lee `CF-Connecting-IP`, que la escribe Cloudflare y sobrescribe lo que mande
el cliente. Antes se tomaba el primer elemento de `X-Forwarded-For`, que
Cloudflare **apila**: bastaba con mandarse uno mismo un `X-Forwarded-For`
inventado para estrenar cubo en cada intento y saltarse el límite del login.

---

## Compilar el APK

Contra el dominio (es lo que sale por defecto, no hace falta pasar nada):

```bash
flutter build apk --release
```

Contra el backend de esta máquina, para desarrollo:

```bash
flutter run --dart-define=GATEWAY_URL=http://10.0.2.2:8080
```

Para un teléfono real por Wi-Fi hay que añadir la IP del PC a
`network_security_config.xml`: esa lista no admite rangos.

---

## Pendiente, cuando lo quieras

- **El panel web** sigue sin publicar. Es otro `hostname` en `config.yml`
  apuntando a donde lo sirvas, y su registro DNS con `tunnel route dns`.
- **Cerrar el acceso.** Hoy el dominio queda abierto a internet y lo único que
  lo protege es el login. Cloudflare Access puede exigir identidad antes de que
  la petición llegue siquiera al gateway.
