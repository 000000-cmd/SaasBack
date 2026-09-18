# Gateway Service - Documentación

## 📋 Descripción
API Gateway que maneja el enrutamiento y autenticación JWT para todos los microservicios de la plataforma SaaS Beauty.

## 🏗️ Arquitectura

```
Cliente → Gateway (Puerto 8080) → Microservicios
                ↓
          - Validación JWT
          - Enrutamiento dinámico
          - CORS
          - Rate limiting
```

## 🔐 Autenticación

### Endpoints Públicos (Sin JWT)
- `/api/auth/login` - Iniciar sesión
- `/api/auth/refresh` - Refrescar token
- `/api/auth/register` - Registro
- `/api/auth/apiV` - Versión de API
- `/api/thirdparties/create` - Crear tercero
- `/eureka/**` - Dashboard de Eureka
- `/actuator/**` - Endpoints de monitoreo

### Endpoints Protegidos
Todos los demás endpoints requieren un token JWT válido en el header:

```http
Authorization: Bearer <token_jwt>
```

## 🚀 Enrutamiento Dinámico

El Gateway descubre automáticamente los servicios registrados en Eureka y crea rutas:

```
http://gateway:8080/{service-name}/** → lb://{SERVICE-NAME}
```

Ejemplos:
- `http://gateway:8080/auth-service/api/users` → `lb://AUTH-SERVICE/api/users`
- `http://gateway:8080/system-service/api/config` → `lb://SYSTEM-SERVICE/api/config`

## 📊 Headers Personalizados

Cuando un token es válido, el Gateway añade headers para los microservicios:

```
X-User-Username: john.doe
X-User-Id: 12345
X-User-Roles: ADMIN,USER
```

Los microservicios pueden leer estos headers para obtener información del usuario autenticado.

## 🛠️ Configuración

### Variables de Entorno
```properties
# JWT
jwt.secret=${JWT_SECRET}   # minimo 32 caracteres, del entorno

# Eureka
eureka.client.service-url.defaultZone=http://discovery-service:8761/eureka/

# Timeouts
spring.cloud.gateway.httpclient.connect-timeout=5000
spring.cloud.gateway.httpclient.response-timeout=5s
```

### Añadir Endpoints Públicos

Edita `RouteValidator.java`:

```java
public static final List<String> PUBLIC_ENDPOINTS = List.of(
    "/api/auth/login",
    "/api/auth/refresh",
    "/tu-nuevo-endpoint"  // Añadir aquí
);
```

## 📈 Monitoreo

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

Respuesta:
```json
{
  "status": "UP",
  "components": {
    "gateway": {
      "status": "UP",
      "details": {
        "jwt": "initialized",
        "eureka": "connected",
        "registered_services": 4
      }
    }
  }
}
```

### Rutas Activas
```bash
curl http://localhost:8080/actuator/gateway/routes
```

## 🧪 Testing

### Test de Login
```bash
curl -X POST http://localhost:8080/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Test de Endpoint Protegido
```bash
TOKEN="tu_token_jwt_aqui"

curl http://localhost:8080/system-service/api/config \
  -H "Authorization: Bearer $TOKEN"
```

## ⚠️ Troubleshooting

### Error: "Missing Authorization header"
- Verifica que estés enviando el header `Authorization: Bearer <token>`
- El endpoint que intentas acceder está protegido

### Error: "Invalid or expired token"
- El token JWT expiró o es inválido
- Refresca el token con `/api/auth/refresh`

### Error: "Service Unavailable"
- El microservicio destino no está registrado en Eureka
- Verifica con `curl http://localhost:8761/eureka/apps`

### Gateway no arranca
```bash
# Ver logs detallados
docker logs -f gateway-service

# Verificar que config-server y discovery estén UP
docker ps | grep -E "(config|discovery)"
```

## 📝 Logs

El Gateway registra todas las peticiones con nivel DEBUG:

```log
DEBUG - Processing request to: /auth-service/api/users
DEBUG - Public endpoint accessed, skipping authentication: /api/auth/login
DEBUG - Token validated successfully for user: john.doe
```

## 🔄 Flujo de Petición

1. Cliente envía petición a `http://gateway:8080/auth-service/api/users`
2. Gateway verifica si la ruta es pública
3. Si es protegida, valida el token JWT
4. Extrae información del usuario y añade headers
5. Enruta la petición a `lb://AUTH-SERVICE/api/users`
6. Eureka resuelve el servicio a una instancia disponible
7. Gateway reenvía la petición
8. Respuesta regresa al cliente

## 🚦 Estado de Servicios

```bash
# Ver servicios registrados
curl http://localhost:8761/eureka/apps | jq

# Ver rutas del gateway
curl http://localhost:8080/actuator/gateway/routes | jq
```

## 📚 Recursos
- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [JWT.io](https://jwt.io) - Debug de tokens
- [Eureka Dashboard](http://localhost:8761)