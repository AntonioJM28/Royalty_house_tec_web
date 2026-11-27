# Lógica MVC de Royalty House

## Arquitectura general

La aplicación implementa MVC con Spring Boot y Thymeleaf, organizado en capas:

```text
Navegador
   │ petición HTTP
   ▼
Controller ── valida formularios y coordina la petición
   │
   ▼
Service ── aplica reglas de negocio y transacciones
   │
   ▼
Repository ── ejecuta consultas con JdbcClient
   │
   ▼
PostgreSQL

Controller ── agrega datos al Model y selecciona una plantilla
   │
   ▼
Thymeleaf ── genera HTML
   │
   ▼
Navegador
```

En este proyecto el término MVC describe la interacción entre controladores, datos del dominio y vistas; los servicios y repositorios separan además la lógica de negocio y el acceso a datos. La aplicación renderiza HTML en el servidor: no hay una API REST separada para las pantallas del cliente.

## Qué corresponde a cada parte

### Controller: entrada y coordinación

Los controladores reciben las solicitudes HTTP, enlazan los parámetros o formularios, ejecutan la validación, invocan servicios y deciden qué vista mostrar o a qué URL redirigir.

- `AutenticacionController` atiende `/`, `/login`, `/registro` y `/logout`. En registro e inicio de sesión usa servicios para autenticar y guarda en sesión únicamente un `UsuarioSesion`.
- `ClienteController` atiende `/cliente/**`: consulta disponibilidad, prepara resúmenes, confirma o cancela reservas, y carga la confirmación de una reserva.
- Los controladores no contienen SQL ni consultan repositorios directamente.

Las clases `LoginForm`, `RegistroForm`, `ReservaBillarForm` y `ReservaMinicineForm` son DTO de entrada para enlazar y validar datos HTTP. No son las plantillas ni reemplazan a los modelos del dominio.

### Model: dominio y datos que se muestran

El paquete `model` contiene las estructuras del dominio, como `Usuario`, `Recurso`, `Reserva`, `Pelicula`, `Tarifa`, `DisponibilidadDia`, `ResumenReserva` y `DetalleReserva`, además de enums como `Rol`, `TipoRecurso` y `EstadoReserva`.

El `Model` de Spring MVC es otra cosa: es el contenedor que el controlador llena con atributos para que Thymeleaf los use al renderizar. Por ejemplo, la pantalla de disponibilidad recibe películas, fecha y datos de disponibilidad; la confirmación recibe un `DetalleReserva`. La sesión web conserva `UsuarioSesion`, que contiene solo datos seguros para identificar al cliente.

### Service: reglas de negocio

Los servicios coordinan operaciones y aplican las reglas antes de leer o escribir datos:

- `AutenticacionService` registra clientes e inicia sesión.
- `DisponibilidadService` obtiene recursos y películas activos, calcula bloques libres, duración y costo, y vuelve a validar los horarios.
- `ReservaService` arma resúmenes, confirma reservas, lista las reservas de un cliente, comprueba si pueden cancelarse y gestiona cancelaciones. Las operaciones de escritura usan transacciones.

Las reglas se comprueban en el servidor aunque la interfaz muestre opciones válidas. Al confirmar una reserva se valida de nuevo el horario; así no se confía en que los datos del formulario o la disponibilidad vista anteriormente sigan vigentes.

### Repository: persistencia

Las clases del paquete `repository` encapsulan el acceso a PostgreSQL mediante `JdbcClient`. Ejecutan las consultas y actualizaciones SQL; no presentan vistas ni deciden por sí solas las reglas de negocio.

`schema.sql` define la estructura de la base y `data.sql` carga datos iniciales. Ambos están en `src/main/resources` para la inicialización de Spring Boot; **no son vistas**. Las vistas HTML están en `src/main/resources/templates`.

## Flujo de una petición

1. El navegador envía una petición HTTP.
2. Spring MVC la despacha al método del controlador correspondiente. Las rutas `/cliente/**` pasan por `ClienteSesionInterceptor`, que exige una sesión de cliente.
3. En una petición con formulario, Spring enlaza los campos al DTO y Jakarta Validation comprueba las restricciones declaradas.
4. El controlador entrega la operación al servicio. El servicio aplica las reglas de negocio y llama a los repositorios.
5. El repositorio consulta o modifica PostgreSQL. Los resultados regresan por la misma cadena hasta el servicio y el controlador.
6. El controlador agrega los datos necesarios al `Model` y devuelve el nombre de una plantilla, o devuelve una redirección. Thymeleaf resuelve la plantilla y genera el HTML.

Las excepciones de negocio se traducen en respuestas de interfaz mediante `GlobalExceptionHandler`; por ejemplo, un horario ocupado redirige a la disponibilidad con un mensaje.

## Flujo MVC de reserva

1. `GET /cliente/disponibilidad` carga las películas y modalidades disponibles. Si se seleccionó un servicio, también consulta horarios y recursos.
2. `cliente/disponibilidad.html` muestra la grilla. Para billar permite elegir un inicio y de uno a cuatro bloques; para minicine, la duración se calcula a partir de la película.
3. El formulario envía la selección a una ruta de resumen. `ClienteController` valida los datos y `ReservaService` vuelve a comprobar el horario y calcula el resumen.
4. `cliente/resumen.html` presenta recurso, fecha, horario, duración y costo estimado. El cliente puede volver o confirmar.
5. Al confirmar, el servicio vuelve a validar, verifica el límite de reservas activas y crea la reserva y sus bloques dentro de una transacción.
6. Si el bloque fue ocupado en paralelo, la restricción única de la base de datos impide duplicarlo y se informa al cliente para que elija otro horario.
7. Tras confirmar, el navegador sigue la redirección a la pantalla de confirmación, que vuelve a obtener los datos de la reserva para el cliente autenticado.

## Rutas y vistas actuales

| Ruta | Acción principal | Vista |
|---|---|---|
| `GET /` | Redirige a la disponibilidad del cliente. | — |
| `GET /registro`, `POST /registro` | Mostrar y procesar el alta de cliente. | `cliente/registro.html` |
| `GET /login`, `POST /login` | Mostrar y procesar el inicio de sesión. | `cliente/login.html` |
| `POST /logout` | Invalidar la sesión y volver al login. | Redirección |
| `GET /cliente/disponibilidad` | Consultar películas, recursos y horarios. | `cliente/disponibilidad.html` |
| `POST /cliente/reservas/billar/resumen` | Validar la selección de billar y preparar el resumen. | `cliente/resumen.html` |
| `POST /cliente/reservas/minicine/resumen` | Validar la selección de minicine y preparar el resumen. | `cliente/resumen.html` |
| `POST /cliente/reservas/billar/confirmar` | Confirmar una reserva de billar. | Redirección a la confirmación |
| `POST /cliente/reservas/minicine/confirmar` | Confirmar una reserva de minicine. | Redirección a la confirmación |
| `GET /cliente/reservas` | Listar las reservas propias y su posibilidad de cancelación. | `cliente/mis-reservas.html` |
| `POST /cliente/reservas/{id}/cancelar` | Cancelar una reserva propia cuando las reglas lo permiten. | Redirección a mis reservas |
| `GET /cliente/reservas/{id}/confirmacion` | Mostrar detalles de una reserva propia. | `cliente/confirmacion.html` |
| Errores de negocio/no encontrado | Presentar un mensaje de error con su estado HTTP. | `error/negocio.html` |

Todas las páginas HTML reutilizan `fragments/layout.html`, que compone el documento, la navegación y el pie de página. Los estilos compartidos están en `static/css/royalty-house.css`; el CSS es un recurso estático, no una vista.

## Estructura relevante

```text
src/main/java/com/royaltyhouse/
├── config/       # Sesión, interceptor y configuración web
├── controller/   # Entrada HTTP y selección de vistas
├── dto/form/     # Datos recibidos y validados desde formularios
├── exception/    # Errores de negocio y manejo global
├── model/        # Dominio y datos de respuesta
├── repository/   # Consultas y persistencia JDBC
└── service/      # Reglas y operaciones de negocio

src/main/resources/
├── application.properties
├── schema.sql                 # Esquema de base de datos
├── data.sql                   # Datos iniciales
├── static/css/                # Recursos estáticos
└── templates/                 # Vistas HTML Thymeleaf
    ├── cliente/
    ├── error/
    └── fragments/
```

El módulo implementado en la interfaz es el del cliente. La presencia de roles o tablas para operaciones administrativas no implica que existan vistas o flujos MVC de administración implementados actualmente.
