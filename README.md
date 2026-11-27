# Royalty House

Sistema web académico para administrar reservas de mesas de billar normales y VIP, salas de minicine y consumos del candy bar.

La aplicación está dividida por roles:

- **CLIENTE:** disponibilidad, creación y cancelación de reservas.
- **RECEPCIONISTA:** grilla operativa, reserva en mostrador, entrada/salida y consumos.
- **ADMINISTRADOR:** recursos, tarifas, películas, horario, personal y reportes.

## Tecnologías

- Java 25.
- Spring Boot 4.1.2 con MVC y Thymeleaf.
- PostgreSQL.
- `JdbcClient` y SQL directo.
- Gradle Wrapper.
- Sesiones HTTP propias; no se utiliza Spring Data JPA ni Spring Security.

## Arquitectura

La organización principal sigue `Controller -> Service -> Repository`:

- Los `Controller` reciben solicitudes HTTP y seleccionan vistas.
- Los `Service` aplican reglas de negocio y transacciones.
- Los `Repository` ejecutan SQL con `JdbcClient`.
- Los `model` son objetos simples: `record` y enumeraciones.
- Las plantillas se encuentran en `src/main/resources/templates`.

## Requisitos

- Java 25.
- PostgreSQL.
- Base de datos llamada `royaltyhouse`.

Configuración principal: `src/main/resources/application.properties`.

```text
jdbc:postgresql://localhost:5432/royaltyhouse
```

Crear la base si todavía no existe:

```sql
CREATE DATABASE royaltyhouse;
```

Al iniciar, `schema.sql` crea o modifica las tablas y `data.sql` carga usuarios y datos de demostración. Los scripts están preparados para desarrollo académico.

## Ejecutar

```powershell
.\gradlew.bat bootRun
```

Abrir [http://localhost:8080](http://localhost:8080).

Comandos útiles:

```powershell
.\gradlew.bat test
.\gradlew.bat compileJava
.\gradlew.bat bootJar
java -jar .\build\libs\royaltyhouse-0.0.1-SNAPSHOT.jar
```

## Cuentas de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@royaltyhouse.local` | `Admin123!` |
| Recepcionista | `recepcion@royaltyhouse.local` | `Recepcion123!` |
| Cliente | `cliente@royaltyhouse.local` | `Cliente123!` |

El usuario determina su redirección: administrador a `/admin`, recepcionista a `/recepcion` y cliente a `/cliente/disponibilidad`.

## Acceso por vistas

- `/cliente/**`: solo `CLIENTE`.
- `/recepcion/**`: `RECEPCIONISTA` y `ADMINISTRADOR`.
- `/admin/**`: solo `ADMINISTRADOR`.

El acceso se controla con interceptores HTTP y el usuario guardado en la sesión, no con Spring Security.

## Configuración configurable

En `application.properties`:

```properties
royaltyhouse.reservas.horas-limite-cancelacion=2
royaltyhouse.reservas.maximo-activas-por-cliente=2
royaltyhouse.reservas.tolerancia-no-presentada-minutos=15
```

## Reglas principales

- Los bloques se representan por horas dentro de `reserva_bloque`.
- Una reserva billar dura entre una y cuatro horas.
- Una reserva minicine usa un bloque por cada hora requerida, redondeando hacia arriba la duración de la película.
- Minicine admite entre uno y siete asistentes.
- Antes de insertar una reserva se vuelve a verificar el horario y los bloques ocupados.
- `UNIQUE(recurso_id, fecha, bloque)` evita duplicar bloques ocupados en PostgreSQL.
- La entrada pasa de `CONFIRMADA` a `EN_USO`; la salida pasa a `FINALIZADA` y libera los bloques.
- No presentada también libera los bloques después de superar la tolerancia configurable.
- Los consumos están permitidos únicamente en reservas minicine `EN_USO`.
- El stock se descuenta al registrar el consumo, dentro de una transacción `@Transactional`.
- Los reportes consideran únicamente reservas `FINALIZADAS`.
- No se registra cobro ni pago.

## Alcance

Incluye cliente, recepcionista, administrador y reportes. No incluye OAuth real con Google, pagos electrónicos, facturación, impresión, notificaciones ni aplicación móvil. El proyecto conserva la autenticación existente por correo y contraseña y utiliza la tabla `usuario` como lista blanca de roles.

## Documentación detallada

- Explicación de backend y vistas: [LOGICA-BACKEND-VISTAS.md](./LOGICA-BACKEND-VISTAS.md).
- Documentación previa de módulos: [DESCRIPCION-MODULOS.md](./DESCRIPCION-MODULOS.md).
