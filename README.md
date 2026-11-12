# Prueba técnica — Plataforma de gestión de pólizas de arrendamiento

Repositorio de entregables para la prueba técnica de desarrollador TI/Sénior. El caso
aborda el diseño y la implementación de una plataforma para gestionar pólizas de
arrendamiento individuales y colectivas, además de ejercicios de optimización SQL, Git y
liderazgo técnico.

## Módulos de la prueba

| Módulo | Tema | Documento |
|---|---|---|
| 1 | Diseño de sistema y arquitectura propuesta | [modulo1_diseno_sistema.md](modulo1_diseno_sistema.md) |
| 2 | Modelo de entidades UML y API funcional | [modulo2_uml_entidades.md](modulo2_uml_entidades.md) |
| 3 | Estrategias de optimización de una consulta SQL | [modulo3_base_datos.md](modulo3_base_datos.md) |
| 4 | Flujo Git para incorporar un cambio específico con `cherry-pick` | [modulo4_git.md](modulo4_git.md) |
| 5 | Liderazgo técnico, organización y gestión del equipo | [modulo5_liderazgo.md](modulo5_liderazgo.md) |

El enunciado original está disponible en [prueba_conocimientos_desarrollador_ti.md](prueba_conocimientos_desarrollador_ti.md).

## Módulo 2 — Proyecto Java

La API está en [`polizas-api/`](polizas-api/), implementada con Java 17, Spring Boot y
Maven Wrapper. Su README contiene los requisitos, comandos para compilar y ejecutar,
endpoints, ejemplos y datos de demostración:

- [Instrucciones de la API](polizas-api/README.md)
- [Colección Postman](polizas-api/postman_collection.json): se adjunta para importar y
  probar los siete endpoints requeridos.
- [Diagrama UML de entidades](modulo2_uml_entidades.md#1-diagrama-de-clases): se incluye
  en el documento del Módulo 2 en formato Mermaid para revisarlo directamente en un visor
  compatible.

## Estructura principal

```text
.
├── modulo1_diseno_sistema.md
├── modulo2_uml_entidades.md
├── modulo3_base_datos.md
├── modulo4_git.md
├── modulo5_liderazgo.md
├── polizas-api/
│   ├── README.md
│   └── postman_collection.json
└── prueba_conocimientos_desarrollador_ti.md
```
