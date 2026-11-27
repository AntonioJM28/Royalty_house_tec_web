# Royalty House: explicación técnica para defensa

Este documento explica qué hace cada parte del sistema y por qué. Está escrito para responder preguntas sobre el flujo HTTP, los servicios, la base de datos, las vistas y las reglas de negocio.

## 1. Arquitectura general

El proyecto usa una arquitectura sencilla de tres capas:

```text
Navegador / formulario Thymeleaf
        -> Controller
        -> Service
        -> Repository
        -> PostgreSQL
```

- El **Controller** recibe parámetros HTTP, carga datos en `Model`, devuelve el nombre de una plantilla o hace redirecciones.
- El **Service** contiene decisiones y validaciones del negocio.
- El **Repository** construye consultas SQL mediante `JdbcClient`.
- Los archivos HTML usan Thymeleaf para presentar los datos enviados por el controlador.

No se usan Spring Data JPA ni repositorios genéricos. Esto hace visible el SQL y facilita explicar el proyecto.

Las entidades principales son `record`; por ejemplo, `Reserva`, `Recurso`, `Usuario`, `Pelicula`, `Producto`, `Tarifa` y `HorarioAtencion`. Son contenedores de datos sin lógica compleja.

## 2. Base de datos

El esquema está en `src/main/resources/schema.sql`. Las tablas relevantes son:

- `usuario`: cuenta, contraseña, rol y estado activo.
- `recurso`: mesas normales, mesas VIP y salas de minicine.
- `tarifa`: precio por hora de billar o precio por persona de minicine.
- `pelicula`: título, duración y si está en cartelera.
- `horario_atencion`: apertura y cierre.
- `reserva`: datos generales de la reserva y su estado.
- `reserva_bloque`: bloque individual ocupado por una reserva.
- `producto`: nombre, precio, stock y activo.
- `consumo`: productos consumidos por una reserva.

`Reserva` puede tener `usuario_id` nulo o asociado a una cuenta. Esto permite que recepcionista reserve para un cliente sin cuenta. También guarda el nombre y teléfono directamente para mantener esos datos históricos.

`asistentes` se usa solamente por minicine. Para billar puede ser nulo.

La restricción `UNIQUE(recurso_id, fecha, bloque)` impide que dos reservas ocupen el mismo bloque del mismo recurso en la misma fecha.

## 3. Inicio de sesión y roles

Archivos principales:

- `AutenticacionController`.
- `AutenticacionService`.
- `UsuarioRepository`.
- `SesionWeb`.
- `ClienteSesionInterceptor`.
- `RolInterceptor`.
- `WebConfig`.

### Inicio de sesión

`AutenticacionController.iniciarSesion` recibe correo y contraseña del formulario. Llama a `AutenticacionService.iniciarSesion`.

El servicio normaliza el correo y pide al repositorio la cuenta. Después comprueba que esté activa y que el hash coincida mediante `HashContrasena`.

Si todo es correcto, crea `UsuarioSesion` y lo guarda en `HttpSession` bajo la clave `usuarioAutenticado`.

`UsuarioSesion` contiene solamente id, nombre, correo y rol. No guarda la contraseña.

### Redirección por rol

`RolInterceptor.rutaPrincipal` devuelve:

- `ADMINISTRADOR` -> `/admin`.
- `RECEPCIONISTA` -> `/recepcion`.
- `CLIENTE` -> `/cliente/disponibilidad`.

La raíz `/` también revisa la sesión y redirige según el rol.

### Protección de rutas

`ClienteSesionInterceptor` protege `/cliente/**`. Requiere un `UsuarioSesion` cuyo rol sea `CLIENTE`.

`RolInterceptor` protege rutas operativas:

- `/admin/**`: solamente administrador.
- `/recepcion/**`: recepcionista o administrador.

Esta protección no modifica ni reemplaza un sistema real de autenticación, pero cumple el alcance académico del proyecto. Google OAuth no está configurado en el proyecto existente, por lo que se conserva el inicio de sesión por correo y contraseña y la tabla `usuario` como lista blanca de roles.

## 4. Disponibilidad

La lógica central está en `DisponibilidadService`.

### Bloques de una hora

Cada día se divide desde la apertura hasta antes del cierre en bloques enteros de una hora.

Por ejemplo, con apertura 10:00 y cierre 23:00 existe la posibilidad de iniciar reservas entre 10:00 y 22:00.

Un recurso puede tener uno o varios bloques ocupados. `ReservaBloqueRepository.listarBloquesOcupados` obtiene los números de hora ocupados para un recurso y fecha.

### Billar

`consultarBillar(fecha, tipo)`:

1. Confirma que el tipo sea mesa normal o VIP.
2. Valida que la fecha no sea anterior a la actual.
3. Obtiene todas las mesas activas del tipo solicitado.
4. Lee sus bloques ocupados.
5. Construye una tabla de horarios y marca libres u ocupados.

El usuario puede seleccionar una duración de uno a cuatro bloques.

### Minicine

`consultarMinicine(fecha, peliculaId)` obtiene la película, calcula su duración redondeando hacia arriba:

```java
int bloques = (duracionMin + 59) / 60;
```

Por ejemplo, una película de 95 minutos ocupa dos bloques. La vista presenta todas las salas activas y marca horarios donde puedan caber esos bloques sin superar el cierre.

### Validación al confirmar

No basta con que la vista muestre disponibilidad. `validarHorario` vuelve a comprobar:

- Fecha válida.
- Hora exacta en punto, sin minutos adicionales.
- Duración positiva.
- Inicio dentro del horario de atención.
- Fin antes o igual al cierre.
- La hora no haya pasado si es el día actual.
- Ningún bloque requerido esté ocupado.

Esto evita aceptar una reserva usando una selección que quedó obsoleta mientras el usuario completaba el formulario.

En la inserción final, `UNIQUE(recurso_id, fecha, bloque)` representa una segunda protección a nivel de base de datos.

## 5. Reserva del cliente

Los archivos principales son:

- `ClienteController`.
- `ReservaService`.
- `ReservaRepository`.
- `ReservaBloqueRepository`.
- `cliente/disponibilidad.html`.
- `cliente/resumen.html`.
- `cliente/confirmacion.html`.
- `cliente/mis-reservas.html`.

### Flujo billar

1. El controlador recibe recurso, fecha, hora y bloques.
2. `resumirBillar` reutiliza `validarHorario`.
3. `DisponibilidadService.calcularCosto` busca la tarifa correspondiente y multiplica precio por hora por bloques.
4. La vista muestra el resumen.
5. Al confirmar, `ReservaService.reservarBillar` verifica nuevamente y crea la reserva.
6. El estado inicial es `CONFIRMADA`.

El usuario no elige modalidad manualmente dentro de billar: el recurso ya es `MESA_NORMAL` o `MESA_VIP`.

### Flujo minicine

1. El cliente selecciona sala y película.
2. El servicio calcula los bloques a partir de la duración.
3. Actualmente usa la misma tarifa por recurso y cantidad de bloques; el modelo de asistentes del cliente no forma parte del método existente del cliente.
4. Se crea la reserva confirmada.

### Cancelación

`ReservaService.cancelar` bloquea la reserva con `FOR UPDATE`, verifica que pertenezca al usuario y que todavía esté `CONFIRMADA`.

Después compara el momento actual con el inicio menos las horas configuradas. Si está dentro del plazo permitido, cambia el estado a `CANCELADA` y elimina los registros de `reserva_bloque` para liberar el horario.

El límite normal es dos horas y se obtiene de `royaltyhouse.reservas.horas-limite-cancelacion`.

## 6. Estados de reserva

`EstadoReserva` contiene:

- `CONFIRMADA`: reserva creada y pendiente de uso.
- `EN_USO`: el cliente ya fue recibido.
- `FINALIZADA`: se registró la salida.
- `CANCELADA`: la reserva fue cancelada dentro del plazo permitido.
- `NO_PRESENTADA`: pasó la tolerancia y no llegó el cliente.

Las transiciones implementadas son:

```text
CONFIRMADA -> EN_USO -> FINALIZADA
CONFIRMADA -> CANCELADA
CONFIRMADA -> NO_PRESENTADA
```

El sistema no cambia estados automáticamente. Los cambios se producen por una acción de usuario o recepcionista.

## 7. Módulo recepcionista

### Arquitectura

`RecepcionistaController` maneja las rutas `/recepcion/**`. `RecepcionistaService` contiene las operaciones y `DisponibilidadService` reutiliza la grilla del cliente.

### Grilla

La vista `recepcion/disponibilidad.html` permite elegir billar o minicine, fecha y recurso/película.

Para billar llama a `consultarBillar`. Para minicine llama a `consultarMinicine`.

La información se presenta por recurso y bloque. Aunque las vistas son diferentes, el cálculo proviene del mismo servicio utilizado por el cliente.

### Registro en mostrador

La vista `nueva-reserva.html` solicita:

- Nombre y teléfono del cliente.
- Identificador opcional de cuenta existente.
- Recurso.
- Película si corresponde a minicine.
- Fecha y hora.
- Duración para billar.
- Asistentes para minicine.

`registrarMostrador` valida primero que nombre y teléfono existan.

Si el recurso es mesa:

- Debe ser normal o VIP.
- Los bloques deben estar entre uno y cuatro.

Si el recurso es sala:

- Debe indicar asistentes entre uno y siete.
- Debe indicar una película disponible.
- La duración se calcula con la duración de la película.

Después vuelve a validar disponibilidad. Genera un código aleatorio de ocho caracteres usando `SecureRandom`, verifica que no exista y crea la reserva con estado `CONFIRMADA`.

Luego inserta un registro en `reserva_bloque` por cada bloque ocupado.

El costo estimado depende del tipo de reserva. Billar usa tarifa por hora por bloques. Minicine usa tarifa por persona por asistentes, como cálculo administrativo; los consumos posteriores se suman al ingreso estimado del reporte.

### Buscar reserva

La recepción puede buscar por:

- Código: búsqueda exacta normalizada a mayúsculas.
- Nombre: búsqueda parcial sin distinguir mayúsculas/minúsculas.

`ReservaRepository.buscarPorCodigo` y `buscarPorNombreCliente` realizan esas consultas.

### Entrada

`marcarEntrada` obtiene la reserva por código y exige:

- Estado `CONFIRMADA`.
- Fecha igual a la fecha actual.
- Hora actual mayor o igual al inicio.
- Hora actual dentro del intervalo de reserva.

Si cumple, actualiza `estado='EN_USO'` y guarda `hora_entrada`.

La implementación no permite marcar entrada antes de la hora inicial.

### Salida

`marcarSalida` exige estado `EN_USO`. Guarda `hora_salida`, cambia el estado a `FINALIZADA` y elimina los bloques de `reserva_bloque`.

Eliminar los bloques representa la liberación del recurso. También permite que una nueva reserva utilice esos horarios.

### No presentada

`marcarNoPresentada` exige:

- Estado `CONFIRMADA`.
- Hora actual posterior a `hora_inicio + tolerancia`.

La tolerancia viene de `royaltyhouse.reservas.tolerancia-no-presentada-minutos`, cuyo valor predeterminado es 15.

El cambio a `NO_PRESENTADA` también elimina los bloques reservados.

### Candy bar y stock

La vista `recepcion/consumos.html` permite escribir cantidades por producto.

`registrarConsumos` comienza obteniendo la reserva bloqueada y comprobando que sea minicine y esté `EN_USO`.

La transacción tiene dos recorridos:

1. Valida existencia y stock suficiente de todos los productos.
2. Si todo es válido, inserta `Consumo` y resta existencias.

Si una cantidad supera el stock, se lanza `ReglaNegocioException`. Como el método está anotado con `@Transactional`, las operaciones anteriores se revierten si aparece una excepción.

El consumo guarda el precio unitario actual. Esto permite conservar el valor cobrado aunque posteriormente cambie la tarifa del producto.

El consumo no descuenta stock al crear la reserva. Solamente ocurre al registrar el consumo.

## 8. Módulo administrador

### Panel

`AdministradorController` expone `/admin/**` y `AdministradorService` prepara los datos mediante los repositorios correspondientes.

Las acciones usan formularios HTML simples y redirecciones `redirect:/admin/...`.

### Mesas y salas

El recurso usa una sola entidad `Recurso`, con campos:

- `nombre`.
- `tipo`: `MESA_NORMAL`, `MESA_VIP` o `SALA_MINICINE`.
- `capacidad`.
- `activo`.

Las vistas separan conceptualmente mesas y salas, pero la persistencia conserva la entidad única indicada por el proyecto para evitar clases innecesarias.

Los datos se pueden crear o actualizar. Para desactivar se cambia `activo=false`.

### Tarifas

`TarifaRepository` consulta y actualiza el precio por tipo de recurso.

En el formulario:

- MESA_NORMAL usa el precio por hora.
- MESA_VIP usa el precio por hora.
- SALA_MINICINE usa el precio por persona.

El campo de base se llama `precio_hora`, aunque para minicine representa precio por persona en la visualización administrativa y en los cálculos del reporte.

### Películas

`Pelicula` tiene título, duración y estado en cartelera. Una película fuera de cartelera no aparece en las búsquedas de disponibilidad mediante `buscarEnCarteleraPorId`.

### Horario

`HorarioAtencionRepository.actualizar` modifica el único horario configurado. Apertura y cierre se usan tanto en la disponibilidad como en el cálculo de horas disponibles de reportes.

El esquema incluye una restricción que exige apertura menor que cierre.

### Candy bar

`ProductoRepository` permite crear, actualizar, desactivar y consultar stock.

El stock no se disminuye al reservar ni al confirmar. Solo `RecepcionistaService.registrarConsumos` ejecuta el descuento.

### Personal y lista blanca

El administrador puede crear cuentas o cambiar el rol y estado de cuentas personales.

Los usuarios del personal se limitan conceptualmente a `RECEPCIONISTA` y `ADMINISTRADOR`. `listarPersonal` filtra esos roles.

Una cuenta nueva usa `HashContrasena.crear` y exige una contraseña de al menos ocho caracteres.

## 9. Reportes

`ReporteRepository` ejecuta consultas sobre reservas finalizadas dentro del rango recibido.

### Ocupación de billar

Horas reservadas:

```sql
SUM(reserva.bloques)
```

para reservas `FINALIZADA` de mesas normales o VIP.

Horas disponibles:

```text
horas de atención × días del rango × cantidad de mesas activas
```

El porcentaje se calcula como:

```text
horas reservadas / horas disponibles × 100
```

### Ocupación de minicine

Suma los bloques de reservas `FINALIZADA` cuya sala sea `SALA_MINICINE`.

Las horas disponibles se calculan con las mismas horas de atención y días, multiplicadas por la cantidad de salas activas.

### Ingresos billar

Para cada reserva finalizada se toma el tipo real del recurso y multiplica:

```text
bloques × precio de la tarifa del tipo
```

Esto contempla por separado mesa normal y mesa VIP.

### Ingresos minicine

Se suma:

```text
precio por persona × asistentes
```

más el valor de todos los consumos registrados:

```text
cantidad × precio_unitario
```

Los consumos se suman aunque pertenezcan a reservas dentro del rango y con estado finalizado.

### Rango vacío o inválido

El controlador usa los últimos 30 días si no recibe parámetros. `ReporteService` rechaza rangos con fecha final anterior a la inicial.

## 10. Vistas Thymeleaf

Las plantillas comparten los fragmentos `fragments/layout.html` para cabecera, navegación y pie.

El diseño reutiliza `royalty-house.css`.

### Cliente

- `login.html`, `registro.html`: autenticación y creación de cuenta.
- `disponibilidad.html`: consulta y selección de horarios.
- `resumen.html`: revisión antes de confirmar.
- `confirmacion.html`: código y datos finales.
- `mis-reservas.html`: reservas propias y cancelación.

### Recepcionista

- `disponibilidad.html`: grilla operativa.
- `nueva-reserva.html`: registro en mostrador.
- `buscar-clientes.html`: búsqueda opcional de cuenta existente.
- `operacion.html`: entrada, salida y no presentada.
- `consumos.html`: registro de candy bar.

### Administrador

- `index.html`: panel principal.
- `mesas.html`: mesas y tipo normal/VIP.
- `peliculas.html`: cartelera.
- `tarifas.html`: precios.
- `horario.html`: apertura y cierre.
- `productos.html`: producto y stock.
- `personal.html`: roles y activo.
- `reportes.html`: tablas por rango y servicio.

## 11. Manejo de errores

Los errores de negocio utilizan excepciones simples como:

- `ReglaNegocioException`: reglas incumplidas.
- `HorarioNoDisponibleException`: conflicto o horario inválido.
- `EntidadNoEncontradaException`: recurso, cuenta o reserva inexistente.
- `CredencialesInvalidasException`: usuario o contraseña incorrecta.

Los controladores también pueden usar `RedirectAttributes` para conservar mensajes durante una redirección.

La clave es diferenciar validación de entrada, validación de disponibilidad y resultado esperado. El usuario recibe un mensaje legible sin exponer detalles SQL.

## 12. Concepto completo de una reserva

Ejemplo de flujo de recepción:

1. Se selecciona una sala y una película.
2. El servicio calcula los bloques requeridos.
3. Se valida que cada bloque esté libre.
4. Se genera el código.
5. Se inserta `reserva` con estado `CONFIRMADA`.
6. Se insertan bloques en `reserva_bloque`.
7. La reserva puede consultarse por código o nombre.
8. Al llegar el cliente, pasa a `EN_USO`.
9. Puede registrar consumos mientras esté `EN_USO`; el stock disminuye.
10. Al salir pasa a `FINALIZADA` y libera los bloques.

Ejemplo de imposibilidad:

- Otra reserva tiene el bloque 10:00 para ese recurso.
- `validarHorario` detecta el bloque ocupado y lanza `HorarioNoDisponibleException`.
- La reserva no se inserta.
- La restricción única también protege ante una segunda petición simultánea.

## 13. Aspectos importantes para la defensa

Posibles preguntas y respuestas cortas:

- **¿Dónde está la lógica de negocio?** En los `Service`.
- **¿Dónde está el SQL?** En los `Repository`.
- **¿Cómo se evita la superposición?** Validación previa, registros por bloque y restricción única.
- **¿Se puede reservar sin volver a validar?** No; el servicio vuelve a comprobar disponibilidad al confirmar.
- **¿Cómo se sabe si una reserva es billar o minicine?** Por el `tipo` del recurso asociado.
- **¿Cómo se cambia un estado?** Mediante métodos del repositorio que ejecutan `UPDATE reserva SET estado=...`.
- **¿Cómo se libera el recurso?** Eliminando los registros de `reserva_bloque`.
- **¿Dónde se descuenta el stock?** Dentro de `registrarConsumos`, con `@Transactional`.
- **¿Se descuenta al reservar?** No.
- **¿Qué reservas aparecen en reportes?** Solo las `FINALIZADAS`.
- **¿Se registran pagos?** No.
- **¿Por qué no se usa JPA?** Se priorizó SQL explícito y simplicidad para el alcance académico.
- **¿Cómo se protegen las rutas?** Con interceptores basados en `UsuarioSesion` y rol.
