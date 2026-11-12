# Policies API — Módulo 2

API de pólizas de arrendamiento con Java 17, Spring Boot 4.1.2 y H2 en memoria.

## Ejecutar

Desde `polizas-api`:

```bash
./mvnw test
./mvnw spring-boot:run
```

En Windows también se puede usar `mvnw.cmd`. La aplicación escucha en `http://localhost:8080`.
Todas las rutas de negocio exigen `x-api-key: 123456` (credencial fija solo para esta prueba).

### Pruebas y parada en Windows

`./mvnw.cmd test` ejecuta JUnit: pruebas del servicio con Mockito, pruebas HTTP con MockMvc y una prueba que levanta Tomcat **en un puerto aleatorio**. No inicia una API permanente en 8080 ni detiene una que ya estaba abierta. `./mvnw.cmd package` también ejecuta las pruebas y genera `target/policies-0.0.1-SNAPSHOT.jar`.

Para ejecutar la API en una terminal interactiva de PowerShell y poder detener **esa misma instancia** con `Ctrl+C`:

```powershell
cd polizas-api
.\mvnw.cmd package
java -jar .\target\policies-0.0.1-SNAPSHOT.jar
```

También se puede usar `.\mvnw.cmd spring-boot:run`, pero el plugin inicia la aplicación en un **proceso Java hijo**: interrumpir Maven desde otra terminal o una sesión que ya se cerró no detiene necesariamente ese hijo. Si el puerto 8080 sigue ocupado, identifica primero qué proceso lo tiene y verifica que sea esta API antes de detenerlo:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
Get-CimInstance Win32_Process -Filter 'ProcessId=PID' | Select-Object ProcessId,ParentProcessId,CommandLine
Stop-Process -Id PID
```

Sustituye `PID` por el número obtenido. **No cierres todos los procesos `java.exe`**: pueden pertenecer a otras aplicaciones. H2 es una base en memoria y se pierde al detener el proceso.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/v1/polizas?tipo=GROUP&estado=ACTIVE` | Lista; filtros opcionales `INDIVIDUAL`/`GROUP` y `ACTIVE`/`RENEWED`/`CANCELLED` |
| GET | `/v1/polizas/{id}/riesgos` | Riesgos de la póliza |
| POST | `/v1/polizas/{id}/renovar` | Renueva por la duración inicial; aplica IPC registrado |
| POST | `/v1/polizas/{id}/cancelar` | Cancela póliza y riesgos |
| POST | `/v1/polizas/{id}/riesgos` | Agrega riesgo **solo a colectiva**: `{"description":"Apartamento 1"}` |
| POST | `/v1/riesgos/{id}/cancelar` | Cancela un riesgo |
| POST | `/core-mock/evento` | Solo registra intento en logs: `{"evento":"ACTUALIZACION","polizaId":555}` |

Los identificadores de póliza y riesgo son UUID. El endpoint de CORE conserva el contrato externo del enunciado.
No hay endpoint de creación de pólizas en el alcance simplificado del Módulo 2; la base H2 inicia sin pólizas.
Para probar la API con datos precargados, activa el perfil `demo` al iniciarla (no se activa en el arranque normal):

```powershell
java -jar .\target\policies-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Antes, ejecuta `.\mvnw.cmd clean verify` para actualizar el JAR. El perfil `demo` carga dos pólizas activas, tres riesgos y un IPC del 3,5 % aplicable al vencimiento de las pólizas (01/01/2027):

| Tipo | ID | Canon inicial | Riesgos |
|---|---|---:|---:|
| GROUP | `01a1ce48-d512-720a-97c8-386d6a328fdf` | 1200,00 | 2 |
| INDIVIDUAL | `01a1ce48-d515-7548-93ed-b7daeed7900f` | 1000,00 | 1 |

Ejemplos de uso en PowerShell:

```powershell
$headers = @{ 'x-api-key' = '123456' }
Invoke-RestMethod http://localhost:8080/v1/polizas -Headers $headers
Invoke-RestMethod http://localhost:8080/v1/polizas/01a1ce48-d512-720a-97c8-386d6a328fdf/riesgos -Headers $headers
Invoke-RestMethod http://localhost:8080/v1/polizas/01a1ce48-d512-720a-97c8-386d6a328fdf/renovar -Method Post -Headers $headers
```

La primera renovación de GROUP produce canon `1242.00` y prima `7452.00`. H2 es volátil: al reiniciar, los datos vuelven a sus valores iniciales. **No uses el perfil `demo` en producción.** La migración de muestra está en `src/main/resources/db/demo/`, separada de las migraciones normales.

En Bash puedes probar las operaciones sin enviar `Idempotency-Key` (es opcional):

```bash
GROUP_ID=01a1ce48-d512-720a-97c8-386d6a328fdf
curl -i -H 'x-api-key: 123456' http://localhost:8080/v1/polizas
curl -i -X POST -H 'x-api-key: 123456' "http://localhost:8080/v1/polizas/$GROUP_ID/renovar"
curl -i -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' \
  -d '{"description":"Apartment 3"}' "http://localhost:8080/v1/polizas/$GROUP_ID/riesgos"
curl -i -X POST -H 'x-api-key: 123456' "http://localhost:8080/v1/polizas/$GROUP_ID/cancelar"
```

Haz la cancelación al final: una póliza cancelada ya no se puede renovar ni recibir riesgos. Para cancelar un riesgo concreto, usa `POST /v1/riesgos/{id}/cancelar` con su ID obtenido de `GET /v1/polizas/{id}/riesgos`.
La tabla `policy.ipc` guarda el porcentaje y su fecha de aplicación; sin IPC registrado se conserva el canon.

## Idempotencia de solicitudes

En operaciones de escritura se puede enviar `Idempotency-Key` (máximo 64 caracteres) con la misma clave en **cada reintento de una misma operación**. La primera petición reserva la clave en H2 antes de ejecutar la operación. Mientras está en curso, las demás reciben `409 Conflict` y `Retry-After: 1`, sin ejecutar la operación; una vez finaliza con 2xx, reciben la respuesta guardada. La misma clave con otro método, ruta o cuerpo responde 409. Un 4xx libera la clave para corregir el pedido; ante un 5xx o una excepción el resultado puede ser desconocido y la clave permanece pendiente: debe verificarse manualmente el efecto antes de liberarla o completar la respuesta. Una clave pendiente **no se libera automáticamente** al vencer el plazo.

No envíes el texto literal `{{$uuid}}` desde Bash: es una expresión de algunas herramientas HTTP, no un UUID generado por Bash. Si decides usar el header, genera una clave **nueva por operación** (`uuidgen` o `cat /proc/sys/kernel/random/uuid` donde esté disponible); usa la misma clave únicamente para reintentar *esa misma* petición.

Se limita a 1 MiB el cuerpo de las peticiones con clave. Las respuestas completadas se retienen 24 horas; después de ese plazo, una repetición puede volver a ejecutar la operación. Como H2 es volátil, reiniciar la aplicación pierde las reservas y respuestas; para una garantía entre reinicios/instancias se requiere una base persistente compartida. Esta idempotencia del cliente no sustituye la integración transaccional con CORE.

## Logs y cobertura

La consola emite JSON (formato Logstash de Spring Boot). Cada petición recibe un `X-Request-Id` generado por el servidor; el mismo `requestId` aparece en los logs de petición y error. Se registran método, estado HTTP y duración (`durationMs`); los errores del controller registran `errorCode`, sin incluir claves, cuerpos, parámetros de consulta ni mensajes de excepciones. No se confía en un `X-Request-Id` enviado por el cliente.

Para ejecutar JUnit y generar el informe de cobertura JaCoCo:

```bash
./mvnw.cmd clean verify
```

Abrir `target/site/jacoco/index.html` para consultar líneas y ramas por clase; `target/site/jacoco/jacoco.xml` permite procesar el resultado automáticamente. Línea base anterior a las pruebas adicionales: **295/341 líneas (86,5 %) y 48/64 ramas (75 %)**. No se ha fijado aún un mínimo de cobertura para fallar el build; debe acordarse antes de añadirlo.

**Alcance:** El mock de CORE no envía actualizaciones reales y la sincronización automática de eventos pertenece al diseño del Módulo 1, no a esta API simplificada.
