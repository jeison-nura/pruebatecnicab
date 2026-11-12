# Módulo 3 — Optimización de bases de datos

## Problema

Se tienen dos tablas de gran tamaño:

- `orders`: 10 millones de registros.
- `customers`: 500.000 registros.

La siguiente consulta busca las órdenes de los clientes de México y presenta un bajo
rendimiento:

```sql
SELECT
    o.order_id,
    o.order_date,
    c.customer_name,
    o.total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'México';
```

Antes de aplicar cambios revisaría el plan de ejecución con `EXPLAIN ANALYZE` —o su
equivalente según el motor— para identificar full table scans, joins costosos, cantidad de
filas leídas, CPU, I/O y diferencias entre las filas estimadas y las reales.

## Estrategias de optimización

### 1. Crear índices adecuados

La consulta filtra por `customers.country` y relaciona las tablas mediante `customer_id`.
Evaluaría estos índices:

```sql
CREATE INDEX idx_customers_country_customer
    ON customers (country, customer_id);

CREATE INDEX idx_orders_customer
    ON orders (customer_id);
```

El primer índice ayuda a localizar los clientes de México y sus identificadores. El
segundo permite encontrar rápidamente las órdenes asociadas a esos clientes. También
verificaría que `customers.customer_id`, al ser clave primaria, tenga un índice.

No crearía un índice por `order_date`, porque esa columna no participa en el filtro ni en
el ordenamiento de la consulta.

Los índices aceleran las lecturas, pero aumentan el almacenamiento y el costo de las
operaciones `INSERT`, `UPDATE` y `DELETE`, por lo que mediría su impacto.

### 2. Actualizar estadísticas y revisar el plan

Actualizaría las estadísticas de las tablas e índices para que el optimizador conozca la
cantidad y distribución real de los datos. También revisaría la selectividad de
`country`.

Si una gran proporción de los clientes pertenece a México, un full table scan podría ser
la opción correcta: recorrer un índice y acceder después a muchas filas puede costar más
que leer la tabla completa. Por eso no asumiría que todo full scan es necesariamente un
error.

También verificaría que no existan conversiones, funciones, espacios o diferencias de
formato sobre `country` que impidan utilizar el índice.

### 3. Reducir los datos procesados y transferidos

Confirmaría que el consumidor necesita todas las columnas seleccionadas. Si alguna no es
necesaria, la eliminaría para reducir lecturas y transferencia de datos.

No reescribiría la consulta usando subconsultas o `EXISTS` sin evidencia. La consulta ya
es sencilla y el optimizador puede elegir el orden más eficiente del `JOIN`. Probaría una
reescritura solo si `EXPLAIN ANALYZE` demuestra que produce un plan mejor.

### 4. Usar una solución de lectura especializada cuando la consulta sea muy frecuente

Si la consulta se ejecuta constantemente y los datos no necesitan actualizarse de forma
inmediata, evaluaría una vista materializada o una tabla de consulta precalculada. Esto
reduce el costo de cada lectura, aunque requiere definir cómo y cuándo actualizar la
información.

También podría utilizar una caché si se repiten las mismas consultas y los datos cambian
poco. Si el problema principal es el volumen de lecturas, consideraría réplicas de
lectura para distribuir la carga sin enviar las escrituras a esas réplicas.

El particionamiento o el sharding serían opciones posteriores. El particionamiento tendría
sentido si existe un criterio adecuado, como `order_date`; el sharding solo lo evaluaría
cuando una única base de datos ya no pueda soportar el volumen, porque introduce mayor
complejidad operativa.

## Validación

Después de cada cambio compararía el plan y el rendimiento antes y después, usando:

- tiempo de ejecución;
- filas y bloques leídos;
- consumo de CPU e I/O;
- latencia bajo carga;
- impacto de los índices en las escrituras;
- almacenamiento adicional.

Las pruebas deben utilizar datos representativos y diferentes niveles de selectividad para
confirmar que la mejora no funciona únicamente para un caso particular.

## Conclusión

Comenzaría con `EXPLAIN ANALYZE`, estadísticas actualizadas e índices sobre
`customers(country, customer_id)` y `orders(customer_id)`. Luego mediría el resultado. Si
la consulta continúa siendo costosa por su frecuencia o volumen, evaluaría caché, vistas
materializadas o réplicas de lectura antes de considerar particionamiento o sharding.
