# Royalty House

Royalty House es una aplicación web académica para administrar reservas de mesas de billar normales y VIP, y salas de minicine. Esta fase implementa el módulo del cliente: creación de cuenta, inicio de sesión, consulta de disponibilidad por bloques de una hora, resumen y confirmación de reservas, consulta de reservas propias y cancelación dentro del plazo permitido.

El backend usa Spring Boot con arquitectura MVC. Las vistas se renderizan con Thymeleaf y el acceso a PostgreSQL se realiza con `JdbcClient` y SQL propio; no se usa JPA/Hibernate. El esquema y los datos de demostración se inicializan desde `schema.sql` y `data.sql`.

## Requisitos

- Java 25 (el proyecto configura ese toolchain).
- PostgreSQL y una base de datos llamada `royaltyhouse`.
- Gradle Wrapper incluido en el proyecto (Gradle 9.9.1).

## Configuración de la base de datos

La conexión está configurada en `src/main/resources/application.properties`. Verifica que la URL, el usuario y la contraseña correspondan a tu instalación local. La configuración actual apunta a:

```text
jdbc:postgresql://localhost:5432/royaltyhouse
```

Si todavía no has creado la base, puedes hacerlo desde `psql` con un usuario que tenga permiso:

```sql
CREATE DATABASE royaltyhouse;
```

Al iniciar la aplicación, `spring.sql.init.mode=always` ejecuta `schema.sql` y `data.sql`. Las tablas usan `IF NOT EXISTS` y los datos de ejemplo evitan duplicar registros en reinicios. Estos scripts son para desarrollo académico; no sustituyen un sistema de migraciones para producción.

## Levantar la aplicación

Desde PowerShell, en la raíz del proyecto:

```powershell
.\gradlew.bat bootRun
```

Abre [http://localhost:8080](http://localhost:8080). La raíz redirige al módulo de cliente; para iniciar sesión, visita [http://localhost:8080/login](http://localhost:8080/login).

Para detener el servidor, pulsa `Ctrl+C` en la terminal donde está ejecutándose `bootRun`.

## Comandos útiles

Ejecutar todas las pruebas:

```powershell
.\gradlew.bat test
```

Compilar el código de producción:

```powershell
.\gradlew.bat compileJava
```

Crear el JAR ejecutable:

```powershell
.\gradlew.bat bootJar
```

Ejecutar el JAR generado (la ruta exacta puede variar con la versión del proyecto):

```powershell
java -jar .\build\libs\royaltyhouse-0.0.1-SNAPSHOT.jar
```

## Cuentas de demostración

El archivo `src/main/resources/data.sql` incluye estas cuentas:

| Rol | Correo | Contraseña de demostración |
|---|---|---|
| Administrador | `admin@royaltyhouse.local` | `Admin123!` |
| Recepcionista | `recepcion@royaltyhouse.local` | `Recepcion123!` |
| Cliente | `cliente@royaltyhouse.local` | `Cliente123!` |

En esta fase solo está implementado el módulo del cliente; las rutas `/cliente/**` requieren iniciar sesión con una cuenta cuyo rol sea `CLIENTE`.

## Nota sobre contraseñas

El proyecto calcula hashes SHA-256 con sal usando `MessageDigest`, sin librerías adicionales, por el alcance académico solicitado. No es una estrategia adecuada para proteger contraseñas en un sistema real: para producción debe usarse un algoritmo adaptativo de derivación de claves, junto con otras medidas de seguridad.

## Alcance actual

Incluye reservas de billar y minicine, con bloques de una hora. En minicine los bloques se calculan redondeando hacia arriba la duración de la película; en billar el cliente elige de uno a cuatro bloques. La base protege contra reservas simultáneas del mismo recurso, fecha y bloque mediante una restricción única.

No incluye pagos electrónicos ni registro de pagos, consumos de candy bar desde el cliente, módulos operativos de recepcionista/administrador, notificaciones ni aplicación móvil.

Para conocer la responsabilidad de cada clase, plantilla y paquete, consulta [DESCRIPCION-MODULOS.md](./DESCRIPCION-MODULOS.md).
