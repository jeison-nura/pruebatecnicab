# Módulo 5 — Liderazgo técnico y gestión

## Caso

Lidero un equipo de ocho desarrolladores. Actualmente, el 40 % de la deuda técnica se
concentra en servicios clave, hubo 10 incidentes críticos durante el último mes y el
negocio espera una nueva funcionalidad en tres semanas. Además, no existen estándares
consistentes, las revisiones de código son irregulares y dos desarrolladores junior tienen
brechas técnicas importantes.

## 1. ¿Cuáles serían mis cinco prioridades en las primeras dos semanas?

Yo haría lo siguiente:

1. **Analizaría los incidentes críticos** para identificar sus causas, servicios afectados
   y patrones recurrentes. Primero atendería los riesgos que todavía puedan afectar a los
   usuarios o comprometer la seguridad.
2. **Revisaría la deuda técnica de los servicios clave** y priorizaría la vinculada con
   incidentes, riesgos de seguridad o bloqueos para la entrega. No intentaría resolver
   toda la deuda de inmediato.
3. **Aclararía con negocio el alcance de la funcionalidad** que debe entregarse en tres
   semanas. Separaría lo imprescindible de lo que podría entregarse después.
4. **Acordaría prácticas mínimas de calidad** para todo el equipo, incluyendo revisión de
   código, pruebas y criterios de terminado.
5. **Conversaría con los dos desarrolladores junior** para entender sus brechas y acordar
   un plan de apoyo con mentoría, tareas graduales y objetivos verificables.

## 2. ¿Cómo organizaría al equipo para mejorar la velocidad y la calidad?

Organizaría el trabajo según las prioridades acordadas: estabilizar los servicios más
problemáticos y entregar primero el alcance esencial de la funcionalidad. Asignaría
responsables claros, limitaría el trabajo simultáneo y haría visibles las dependencias y
los bloqueos.

También reservaría capacidad para corregir las causas recurrentes de incidentes, en lugar
de posponer indefinidamente la deuda técnica. Para las tareas de mayor riesgo y para
apoyar a los juniors, promovería el trabajo en pares y la mentoría. Revisaría el avance
con el equipo cada semana y ajustaría la distribución del trabajo según los resultados.

## 3. ¿Qué métricas implementaría para evaluar el desempeño del área?

Mediría tendencias del equipo y del servicio para identificar problemas y decidir dónde
actuar. Seguiría:

- La cantidad y severidad de incidentes, su recurrencia y el tiempo de recuperación.
- El tiempo que tarda un cambio desde que está listo hasta que se entrega.
- La frecuencia de entregas y cuántas requieren corrección o rollback.
- Los defectos que llegan a producción y los resultados de las pruebas.
- El trabajo completado frente al comprometido y el tiempo que permanece bloqueado.
- La evolución de los riesgos técnicos prioritarios.

Primero establecería una línea base y después revisaría si las métricas mejoran. No usaría
métricas aisladas, como líneas de código o cantidad de commits, para comparar la
productividad individual.

## 4. ¿Qué prácticas técnicas establecería como obligatorias?

Establecería pull requests para los cambios, revisión por otra persona y revisión de un
senior para cambios de alto riesgo. También exigiría pruebas adecuadas al cambio y que los
checks de integración continua pasen antes de integrar.

Acordaría estándares de código y una definición de terminado que incluya pruebas y
documentación relevante. Procuraría que los cambios sean pequeños y fáciles de revisar,
y que las refactorizaciones amplias no se mezclen con una entrega urgente. Para cada
incidente importante, registraría las causas y las acciones preventivas, con responsables.

## 5. ¿Cómo gestionaría la presión del negocio sin comprometer la calidad?

Conversaría pronto con negocio para confirmar el resultado que realmente necesita en tres
semanas. Propondría entregar primero un producto mínimo viable, con criterios de aceptación
claros, y planificar el resto en entregas posteriores.

Presentaría las opciones y sus consecuencias sobre alcance, fecha y riesgo. No prometería
una combinación de alcance y plazo que el equipo no pueda cumplir de manera responsable.
Si quedara un riesgo residual, lo explicaría junto con las medidas para reducirlo y
acordaría cómo proceder.

Para los desarrolladores junior, definiría expectativas claras, mentoría y puntos de
seguimiento. Si no observara progreso sostenido después de brindar apoyo y
retroalimentación, conversaría con ellos y seguiría el proceso formal de desempeño
correspondiente. No tomaría la salida del equipo como primera medida.
