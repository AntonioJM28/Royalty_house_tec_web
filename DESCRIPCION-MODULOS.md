# Descripción de módulos y clases

El código Java se encuentra bajo `src/main/java/com/royaltyhouse`. Las vistas y recursos web están bajo `src/main/resources`. La aplicación sigue MVC: el controlador recibe solicitudes web y prepara el modelo para Thymeleaf; el servicio contiene las reglas de negocio; el modelo representa el dominio y los datos que la vista necesita.

## Flujo general MVC

1. El navegador envía una petición a un controlador.
2. Para las rutas `/cliente/**`, `ClienteSesionInterceptor` exige una sesión con rol `CLIENTE`.
3. El controlador valida los formularios y llama a uno o más servicios. No accede directamente a repositorios.
4. El servicio aplica las reglas y usa repositorios transaccionales para consultar o actualizar PostgreSQL mediante `JdbcClient`.
5. El controlador entrega atributos a una plantilla Thymeleaf o redirige a otra ruta.

## Modelo (`model`)

Los records son objetos de datos inmutables. Los enums limitan los valores del dominio a los estados y tipos previstos.

| Clase | Responsabilidad |
|---|---|
| `Usuario` | Datos de una cuenta, incluido el hash de contraseña, rol, estado activo y aceptación del tratamiento de datos. El hash no debe enviarse a las vistas. |
| `UsuarioSesion` | Proyección segura del usuario autenticado para conservar en `HttpSession`: identificador, nombre, correo y rol; no contiene credenciales. |
| `Rol` | Roles admitidos: `CLIENTE`, `RECEPCIONISTA` y `ADMINISTRADOR`. En esta fase las rutas de cliente solo aceptan `CLIENTE`. |
| `Recurso` | Mesa o sala: identificador, nombre, tipo, capacidad y estado activo. |
| `TipoRecurso` | Tipos de recurso: `MESA_NORMAL`, `MESA_VIP` y `SALA_MINICINE`. |
| `Tarifa` | Tarifa por tipo de recurso, con precio por hora en `BigDecimal`. |
| `HorarioAtencion` | Horas de apertura y cierre configuradas en base de datos. |
| `Pelicula` | Título, duración en minutos y estado de cartelera. |
| `Reserva` | Datos persistidos de una reserva: código, cliente, recurso, fecha, inicio, bloques, estado y horas de entrada/salida. El identificador de película es nullable para billar. |
| `EstadoReserva` | Estados `CONFIRMADA`, `EN_USO`, `FINALIZADA`, `CANCELADA` y `NO_PRESENTADA`. |
| `ReservaBloque` | Representación de un bloque de una reserva. La base de datos impide duplicar recurso, fecha y número de bloque. |
| `Producto` | Producto del candy bar, su precio, existencias y estado. Se incluye para el esquema general, aunque el flujo del cliente no vende productos. |
| `Consumo` | Consumo de un producto asociado a una reserva. Pertenece a la fase de operación del local y no se registra al reservar. |
| `BloqueDisponibilidad` | Estado mostrado para una hora: si está libre y si puede ser el inicio de una reserva de la duración solicitada. |
| `DisponibilidadRecurso` | Agrupa un recurso con su lista de bloques horarios del día consultado. |
| `DisponibilidadDia` | Resultado de disponibilidad para una fecha, categoría, película opcional, duración necesaria y recursos. |
| `ResumenReserva` | Datos previos a confirmar: recurso, película opcional, fecha, horas, cantidad de bloques y costo estimado. |
| `DetalleReserva` | Datos de confirmación: reserva persistida, recurso, película opcional, hora final y costo estimado. |

## Servicios (`service`)

Los servicios concentran las reglas de negocio para que puedan reutilizarse más adelante desde otros módulos MVC.

### `AutenticacionService`

- `registrarCliente(...)`: exige aceptación de datos, valida los campos principales, normaliza el correo y registra una cuenta de rol `CLIENTE`.
- `iniciarSesion(...)`: busca por correo normalizado, exige una cuenta activa y compara la contraseña con el hash guardado.
- Traduce una violación de correo único a `CuentaYaRegistradaException` y no devuelve el hash en el objeto de sesión.
- Sus operaciones de escritura y lectura se marcan transaccionalmente.

### `HashContrasena`

Utilidad de paquete para crear y verificar hashes. Genera una sal aleatoria de 16 bytes, la codifica en hexadecimal y calcula SHA-256 sobre `sal:contraseña`. El formato guardado es `salHex:hashHex`. Esta implementación se usa por la restricción académica; no es apropiada para un sistema real.

### `DisponibilidadService`

- Lista películas activas en cartelera.
- `consultarBillar(...)` filtra por mesa normal o VIP; `consultarMinicine(...)` busca la película y obtiene las salas.
- Calcula los bloques de minicine como `ceil(duraciónMin / 60)`.
- Construye la grilla de cada recurso considerando horario de atención, bloques ocupados y horas que aún no han comenzado.
- `validarHorario(...)` vuelve a comprobar fecha, alineación a la hora, apertura/cierre y todos los bloques solicitados antes de reservar. Esta segunda validación evita confiar en la grilla que pudo haber visto el usuario anteriormente.
- Obtiene el recurso activo, la película vigente, la tarifa y el horario de atención.

La vista identifica un bloque como inicio seleccionable solo si todos los bloques consecutivos requeridos están libres y el fin cae dentro del horario de cierre.

### `ReservaService`

- `resumirBillar(...)` y `resumirMinicine(...)` preparan el resumen y validan de nuevo el horario antes de mostrarlo.
- `reservarBillar(...)` comprueba que la duración sea de 1 a 4 bloques y que el recurso sea una mesa.
- `reservarMinicine(...)` exige una sala y película en cartelera; obtiene los bloques desde la duración de la película.
- Ambos métodos de reserva vuelven a validar disponibilidad, verifican el máximo configurable de reservas futuras y crean una reserva confirmada con un código aleatorio de ocho caracteres.
- Inserta una fila por bloque ocupado. Si otra transacción toma el mismo bloque, la restricción única de PostgreSQL hace fallar la operación; la transacción revierte también la fila de reserva.
- Bloquea la fila del usuario durante la confirmación para serializar reservas simultáneas de la misma cuenta y evitar exceder el límite de dos reservas activas.
- `listarMisReservas(...)` obtiene las reservas propias; `obtenerDetalle(...)` verifica propiedad antes de entregar los detalles.
- `puedeCancelar(...)` comprueba estado y plazo configurable. `cancelar(...)` bloquea la reserva, verifica propiedad, estado y límite, cambia el estado a `CANCELADA` y borra sus bloques en una misma transacción.

### `RelojConfig`

Publica un `Clock` del sistema. Los servicios lo usan para la lógica temporal, y las pruebas pueden reemplazarlo por un reloj fijo para ser deterministas.

## Controladores (`controller`)

### `AutenticacionController`

Controla `/`, `/login`, `/registro` y `/logout`. Muestra y procesa los formularios de login/registro, aplica validación Jakarta, delega autenticación al servicio y almacena en sesión únicamente `UsuarioSesion`. Al autenticar invalida una sesión anterior y cambia el identificador de sesión. El logout invalida la sesión actual.

### `ClienteController`

Controla `/cliente/**`:

- `GET /cliente/disponibilidad`: carga películas y tipos de billar; recibe servicio, fecha, tipo o película y muestra la grilla resultante.
- `POST /cliente/reservas/billar/resumen` y `POST /cliente/reservas/minicine/resumen`: validan la selección, calculan el resumen y presentan la plantilla correspondiente.
- `POST /cliente/reservas/billar/confirmar` y `POST /cliente/reservas/minicine/confirmar`: leen la cuenta de la sesión, llaman al servicio de reserva y redirigen a la confirmación. Si el bloque se ocupó, vuelven a la grilla con un aviso.
- `GET /cliente/reservas`: carga las reservas del usuario y calcula cuáles aún se pueden cancelar.
- `POST /cliente/reservas/{id}/cancelar`: solicita cancelación para el usuario autenticado y muestra el resultado.
- `GET /cliente/reservas/{id}/confirmacion`: comprueba que la reserva pertenezca a la cuenta antes de presentar sus detalles.

El controlador no contiene SQL ni accede a repositorios.

## Vistas Thymeleaf

### Fragmento compartido

- `templates/fragments/layout.html`: fragmento `layout(title, content)` que compone el documento HTML, encabezado con navegación, contenido de la página y pie. Carga la hoja de estilos común.

### Vistas del cliente

| Plantilla | Uso |
|---|---|
| `templates/cliente/login.html` | Formulario de correo y contraseña; presenta errores de autenticación y mensajes de sesión. |
| `templates/cliente/registro.html` | Formulario para nombre, correo, teléfono, contraseña y aceptación obligatoria del tratamiento de datos. |
| `templates/cliente/disponibilidad.html` | Selector de servicio/fecha y modalidad o película. Renderiza la grilla por recurso, distingue bloques libres y ocupados, y permite elegir un inicio y duración válidos. |
| `templates/cliente/resumen.html` | Muestra recurso, modalidad o película, fecha, horario, duración y costo estimado; ofrece volver o confirmar. Incluye el aviso de pago en efectivo. |
| `templates/cliente/confirmacion.html` | Presenta el código generado, los detalles de la reserva y el aviso de pago en efectivo en el local. |
| `templates/cliente/mis-reservas.html` | Lista las reservas del cliente con estado y permite cancelar solo cuando el plazo sigue vigente. |

### Vista de errores

- `templates/error/negocio.html`: mensaje para errores de regla de negocio o entidad no encontrada.

### Estilos

- `static/css/royalty-house.css`: estilos responsive sencillos para formularios, tarjetas, grilla de horarios, avisos y estados. No usa frameworks JavaScript ni CSS.

## Repositorios (`repository`)

Cada repositorio encapsula SQL con `JdbcClient`; no implementa reglas de negocio ni decide permisos.

| Clase | Acceso principal |
|---|---|
| `UsuarioRepository` | Buscar cuentas por correo o identificador, bloquear una cuenta con `FOR UPDATE` e insertar clientes. |
| `RecursoRepository` | Listar recursos activos por tipo y encontrar un recurso activo por identificador. |
| `TarifaRepository` | Buscar la tarifa de un tipo de recurso. |
| `HorarioAtencionRepository` | Obtener el horario configurado. |
| `PeliculaRepository` | Listar películas en cartelera y buscar una película activa por identificador. |
| `ReservaRepository` | Insertar, buscar y listar reservas; contar reservas confirmadas futuras; bloquear filas y actualizar estados. |
| `ReservaBloqueRepository` | Consultar bloques ocupados, insertar cada bloque y liberarlos al cancelar. |
| `ProductoRepository` | Consultar productos activos para el módulo del local. |
| `ConsumoRepository` | Insertar consumos y listarlos por reserva; no se utiliza en el flujo actual del cliente. |

## Formularios (`dto/form`)

Son objetos de transporte para recibir datos web, distintos de las entidades del dominio. Sus anotaciones Jakarta Validation detectan formatos y campos faltantes antes de invocar el servicio.

- `LoginForm`: correo y contraseña.
- `RegistroForm`: nombre, correo, teléfono, contraseña y aceptación de datos.
- `ReservaBillarForm`: recurso, fecha, hora inicial, duración de 1 a 4 bloques y tipo de mesa.
- `ReservaMinicineForm`: recurso, película, fecha y hora inicial; la duración se determina en el servicio.

Las reglas de negocio se vuelven a comprobar en los servicios, incluso si un valor pasa la validación del formulario.

## Configuración (`config`)

- `ClienteSesionInterceptor`: permite únicamente solicitudes con una sesión que contiene un `UsuarioSesion` de rol `CLIENTE`; si la sesión falta o tiene otro rol, invalida la sesión existente (si la hay) y redirige al login.
- `WebConfig`: registra el interceptor para `/cliente/**`.
- `SesionWeb`: centraliza el nombre del atributo de sesión.
- `RelojConfig`: registra el `Clock` del sistema utilizado por las reglas temporales.

## Excepciones (`exception`)

- `ReglaNegocioException`: error base para una operación que infringe una regla del negocio.
- `HorarioNoDisponibleException`: indica que el bloque no está disponible, cae fuera del horario o ya empezó.
- `CredencialesInvalidasException`: respuesta común para correo inexistente, cuenta inactiva o contraseña incorrecta.
- `CuentaYaRegistradaException`: indica que el correo ya está registrado.
- `EntidadNoEncontradaException`: informa que una cuenta, recurso, película o reserva requerida no existe o no es accesible.
- `GlobalExceptionHandler`: traduce errores de negocio a una vista con estado HTTP 400, entidades no encontradas a HTTP 404 y duplicados de clave a una redirección de disponibilidad con un mensaje de horario ocupado.

## Aplicación y pruebas

- `com.royaltyhouse.royaltyhouse.RoyaltyhouseApplication`: punto de entrada Spring Boot. Declara el escaneo de componentes en `com.royaltyhouse` para detectar todos los paquetes MVC.
- `src/test/java/com/royaltyhouse/service/ReservaServiceTest.java`: pruebas unitarias con Mockito para superposición, límite de reservas y cancelación tardía.
- `com.royaltyhouse.royaltyhouse.RoyaltyhouseApplicationTests`: prueba de carga del contexto Spring; con `spring.sql.init.mode=always`, también requiere que PostgreSQL esté disponible y correctamente configurado.

## Persistencia y dependencias

- `src/main/resources/schema.sql`: tablas de usuarios, recursos, tarifas, horario, películas, reservas, bloques, productos y consumos, con claves foráneas y restricciones.
- `src/main/resources/data.sql`: recursos y cuentas de demostración, más películas y productos iniciales. Las inserciones son idempotentes frente a los identificadores naturales definidos.
- `src/main/resources/application.properties`: conexión a PostgreSQL, inicialización de SQL, timeout de sesión y parámetros configurables del negocio.
- `build.gradle`: configura Java 25, Spring Boot, MVC, JDBC, Thymeleaf, validación, PostgreSQL y pruebas.

Para comandos de ejecución y cuentas de demostración, consulta [README.md](./README.md).
