# Módulo 4 — Incorporar un cambio específico con Git

## Problema

Estoy trabajando en `feature/new-login` y un cambio crítico de seguridad ya fue integrado
en `main`. Necesito traer **solo ese cambio** a mi rama, sin incorporar el resto de los
commits de `main`.

## Estrategia

Usaría `git cherry-pick` sobre el commit que contiene la corrección. Este comando aplica
los cambios introducidos por ese commit a la rama actual y crea un commit nuevo en ella.
No fusiona toda la rama `main`.

## Flujo de trabajo, comando por comando

### 1. Actualizar la información del repositorio remoto

```bash
git fetch origin
```

Descarga los commits y referencias actualizadas del remoto `origin`, sin cambiar la rama
actual ni modificar los archivos de trabajo.

### 2. Cambiar a la rama que recibirá la corrección

```bash
git switch feature/new-login
```

Deja activa la rama de trabajo. `cherry-pick` aplicará el cambio sobre la rama actual,
por eso es importante verificar que se está en `feature/new-login` antes de continuar.

### 3. Verificar que el árbol de trabajo esté limpio

```bash
git status
```

Confirmaría que no hay cambios locales pendientes que puedan mezclarse con la operación.
Si los hay, los guardaría en un commit propio o los apartaría antes de continuar.

### 4. Encontrar e inspeccionar el commit de seguridad

```bash
git log --oneline origin/main
git show --stat <hash-del-commit>
```

El primer comando muestra los commits disponibles en la referencia actualizada de `main`.
El segundo permite revisar el commit elegido y los archivos que modifica. Antes de
aplicarlo confirmaría que contiene la corrección necesaria y que no depende de otros
cambios de `main`.

### 5. Aplicar solo ese commit

```bash
git cherry-pick <hash-del-commit>
```

Git aplica el cambio a `feature/new-login` y, si termina correctamente, crea un commit
nuevo. El commit original permanece en `main`; el commit creado en la rama de trabajo
tendrá un hash distinto, aunque represente el mismo cambio.

### 6. Resolver conflictos, si aparecen

Si Git informa conflictos, primero revisaría qué archivos necesitan atención:

```bash
git status
```

Resolvería los marcadores de conflicto en los archivos, comprobaría que la corrección de
seguridad se conserva y luego marcaría los archivos resueltos:

```bash
git add <archivo-resuelto>
git cherry-pick --continue
```

`git cherry-pick --continue` finaliza la operación y registra el commit nuevo. Si decido
cancelarla y volver al estado anterior al intento:

```bash
git cherry-pick --abort
```

### 7. Verificar y publicar la rama

```bash
git show --stat --oneline HEAD
```

Después ejecutaría las pruebas correspondientes y revisaría el diff. Si todo está bien,
publicaría la rama:

```bash
git push origin feature/new-login
```

Si el repositorio protege las ramas, abriría un pull request para revisión e integración.

## Consideración

`cherry-pick` incorpora un commit, no sus dependencias automáticamente. Si la corrección
requiere otros cambios que también están en `main`, identificaría e incorporaría esos
commits necesarios de forma explícita, o revisaría si conviene otra estrategia. Si el
commit seleccionado es un merge commit, se debe elegir su línea principal con `-m`.

## Resumen

```bash
git fetch origin
git switch feature/new-login
git status
git log --oneline origin/main
git show --stat <hash-del-commit>
git cherry-pick <hash-del-commit>
# Resolver conflictos, si los hay; luego:
git add <archivo-resuelto>
git cherry-pick --continue
# Ejecutar pruebas y revisar el cambio
git push origin feature/new-login
```
