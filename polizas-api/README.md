# Policies API — Módulo 2

API REST para gestionar pólizas de arrendamiento individuales y colectivas.
Implementa el alcance práctico del Módulo 2 de la prueba técnica.

## 1. Alcance

La API permite:

- consultar pólizas y sus riesgos;
- renovar pólizas aplicando el IPC;
- cancelar pólizas y sus riesgos;
- cancelar riesgos individualmente;
- agregar riesgos únicamente a pólizas colectivas;
- simular el envío de eventos al CORE legado;
- proteger las rutas mediante `x-api-key`.

La creación de pólizas no tiene endpoint en el alcance simplificado del enunciado.
Por eso se incluyen datos de prueba mediante el perfil `demo`.

## 2. Stack y estructura

- Java 17
- Spring Boot 4.1.2
- Maven Wrapper 3.10.0
- Spring MVC, Spring Data JPA, Spring Security y Bean Validation
- Flyway para migraciones
- H2 en memoria
- JUnit 5, Mockito, MockMvc y JaCoCo

La estructura sigue las capas solicitadas por la prueba:

```text
controller  →  service  →  repository
     ↓             ↓           ↓
    REST      reglas de negocio   JPA/H2
```

## 3. ¿Por qué usamos H2?

El Módulo 2 pide implementar únicamente lo esencial y debe poder ejecutarse sin
infraestructura externa. Por eso usamos H2 en memoria: permite que cualquier persona
compile, pruebe y ejecute la API rápidamente con solo Java y el Maven Wrapper.

H2 es suficiente para demostrar los endpoints, las reglas de negocio, las transacciones
y las pruebas automatizadas. No representa la persistencia productiva: al detener la
aplicación se pierden los datos y una instancia de H2 no sirve para compartir estado
entre varias réplicas.

En la propuesta del Módulo 1, PostgreSQL en Docker es la base de referencia para
persistencia real, réplicas y pruebas de carga. Esa decisión es independiente de esta
implementación mínima del Módulo 2.

## 4. Requisitos

Para ejecutar la API normalmente se necesita:

1. Java 17 instalado y disponible en `PATH`.
2. Git Bash, Linux/macOS Terminal o PowerShell.
3. Acceso al repositorio local.
4. Puerto `8080` disponible.

No se requiere Maven instalado: el proyecto incluye `mvnw` y `mvnw.cmd`.
No se requiere Docker para el Módulo 2.

Verificar Java desde Bash:

```bash
java -version
```

## 5. Compilar y ejecutar

### Bash, Linux o macOS

Desde la raíz del repositorio:

```bash
cd polizas-api
./mvnw clean verify
java -jar target/policies-0.0.1-SNAPSHOT.jar
```

### Windows PowerShell

```powershell
cd polizas-api
.\mvnw.cmd clean verify
java -jar .\target\policies-0.0.1-SNAPSHOT.jar
```

La aplicación queda disponible en:

```text
http://localhost:8080
```

Para detener la aplicación ejecutada con `java -jar`, presiona `Ctrl+C` en esa misma
terminal. Al usar H2 en memoria los datos se reinician al parar.

## 6. Ejecutar con datos de prueba

El perfil `demo` carga dos pólizas, tres riesgos y un IPC del 3,5 %. No se activa por
defecto.

En Bash:

```bash
cd polizas-api
./mvnw clean verify
java -jar target/policies-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

En PowerShell:

```powershell
java -jar .\target\policies-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Datos disponibles:

| Tipo | ID | Canon inicial | Riesgos |
|---|---|---:|---:|
| `GROUP` | `01a1ce48-d512-720a-97c8-386d6a328fdf` | 1200.00 | 2 |
| `INDIVIDUAL` | `01a1ce48-d515-7548-93ed-b7daeed7900f` | 1000.00 | 1 |

El IPC de muestra es 3,5 % y aplica a las renovaciones de estas pólizas. La primera
renovación de la póliza `GROUP` produce canon `1242.00` y prima `7452.00`.

El perfil y sus datos están en:

```text
src/main/resources/application-demo.yml
src/main/resources/db/demo/V1794462287255__seed_demo_policies.sql
```

No usar el perfil `demo` en producción.

## 7. Endpoints

Todas las rutas de negocio requieren el header:

```http
x-api-key: 123456
```

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/v1/polizas` | Lista pólizas con filtros `tipo`, `estado` y paginación `page`, `size`. |
| `GET` | `/v1/polizas/{id}/riesgos` | Lista los riesgos de una póliza con `page` y `size`. |
| `POST` | `/v1/polizas/{id}/renovar` | Renueva por el periodo inicial y aplica el IPC. |
| `POST` | `/v1/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos activos. |
| `POST` | `/v1/polizas/{id}/riesgos` | Agrega un riesgo únicamente a una póliza `GROUP`. |
| `POST` | `/v1/riesgos/{id}/cancelar` | Cancela un riesgo específico. |
| `POST` | `/core-mock/evento` | Registra en logs el intento de envío al CORE. |
| `GET` | `/actuator/health` | Health check; no requiere API key. |

Valores válidos:

- `tipo`: `INDIVIDUAL` o `GROUP`.
- `estado`: `ACTIVE`, `RENEWED` o `CANCELLED`.

Los dos listados aceptan `page` (desde `0`, por defecto `0`) y `size` (de `1` a `100`,
por defecto `20`). Parámetros inválidos devuelven `400`; páginas fuera del rango
devuelven `content` vacío. Los resultados se ordenan por `createdAt` e `id` ascendente
para mantener un orden estable. El formato de respuesta es:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

**Cambio de contrato:** anteriormente los GET devolvían arreglos JSON directamente;
ahora los datos están dentro de `content` junto con metadatos de paginación. Los clientes
existentes deben adaptar la lectura de la respuesta (para una API publicada, esto
requeriría una nueva versión o una transición de compatibilidad).

Ejemplo de listado:

```bash
curl -H 'x-api-key: 123456' \
  'http://localhost:8080/v1/polizas?tipo=GROUP&estado=ACTIVE&page=0&size=20'
```

Ejemplo de agregar riesgo:

```bash
GROUP_ID=01a1ce48-d512-720a-97c8-386d6a328fdf
curl -X POST -H 'x-api-key: 123456' \
  -H 'Content-Type: application/json' \
  -d '{"description":"Apartment 3"}' \
  "http://localhost:8080/v1/polizas/$GROUP_ID/riesgos"
```

Para consultar los IDs de los riesgos:

```bash
curl -H 'x-api-key: 123456' \
  "http://localhost:8080/v1/polizas/$GROUP_ID/riesgos"
```

La cancelación de la póliza debe hacerse al final: una póliza cancelada no puede
renovarse ni recibir nuevos riesgos.

## 8. Reglas de negocio

- Una póliza individual solo puede tener un riesgo.
- Los riesgos nuevos solo se agregan a pólizas colectivas (`GROUP`).
- No se puede renovar una póliza cancelada.
- Al cancelar una póliza se cancelan sus riesgos activos en la misma transacción.
- La prima se calcula como canon mensual por meses de vigencia.
- La renovación conserva el periodo inicial y ajusta el canon según el IPC aplicable.
- Las transiciones concurrentes se protegen con transacciones y bloqueo de la póliza.

## 9. Idempotencia

`Idempotency-Key` es opcional para operaciones de escritura y admite hasta 64 caracteres.
Debe generarse una clave nueva para cada operación distinta y reutilizarse únicamente al
reintentar esa misma operación.

- La primera solicitud reserva la clave antes de ejecutar el cambio.
- Las solicitudes concurrentes con la misma clave reciben `409 Conflict` y
  `Retry-After: 1`, sin ejecutar la operación otra vez.
- Una repetición posterior a un `2xx` recibe la respuesta guardada.
- Reutilizar la clave con otro método, ruta o cuerpo produce `409 Conflict`.
- Un `4xx` libera la clave para corregir la petición.
- Después de un `5xx` o una excepción, la clave queda pendiente porque el efecto puede
  haber ocurrido; no se libera automáticamente.
- Las respuestas completadas se conservan durante 24 horas.
- El cuerpo de una solicitud con idempotencia está limitado a 1 MiB.

En Bash no se debe enviar literalmente `{{$uuid}}`: esa expresión la reemplaza Postman,
pero Bash la enviaría como texto. Se puede generar una clave con:

```bash
IDEMPOTENCY_KEY=$(uuidgen)
```

## 10. Logs y observabilidad

La consola emite logs JSON en formato Logstash. Cada petición recibe un `X-Request-Id`
generado por el servidor. Los logs de petición incluyen:

- `requestId`;
- método HTTP;
- estado de respuesta;
- duración en milisegundos.

Los errores registran códigos sanitizados. No se registran API keys, cuerpos completos,
parámetros de consulta ni mensajes de excepciones que puedan contener datos del cliente.

## 11. Pruebas y cobertura

Ejecutar la suite completa:

```bash
./mvnw test
```

Ejecutar pruebas y generar el informe JaCoCo:

```bash
./mvnw clean verify
```

El informe HTML queda en:

```text
target/site/jacoco/index.html
```

La suite cubre pruebas unitarias con Mockito, pruebas HTTP con MockMvc, concurrencia de
idempotencia, arranque real en puerto aleatorio, datos demo y privacidad de logs.
La última verificación ejecutó **41 pruebas sin fallos** y alcanzó aproximadamente **91 %
de cobertura de líneas** y **81 % de ramas**.

## 12. Limitaciones deliberadas

Este proyecto implementa el alcance mínimo del Módulo 2. Por tanto:

- H2 no ofrece persistencia real ni alta disponibilidad.
- No existe un endpoint de creación o modificación general de pólizas porque no lo exige
  el caso simplificado.
- `/core-mock/evento` solo registra el intento; no integra realmente con WebLogic o CORE.
- Outbox, workers, notificaciones, scheduler automático y circuit breaker pertenecen a
  la propuesta arquitectónica del Módulo 1, no a esta implementación mínima.
- Para producción se usaría PostgreSQL compartido, gestión segura de secretos,
  autenticación más robusta que una API key fija y un gateway delante de la aplicación.

La colección de Postman está disponible en `postman_collection.json`.
