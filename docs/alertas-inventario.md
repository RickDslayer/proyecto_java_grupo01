# Issue #34 — Alertas de inventario (Observer)

## Propósito

Consultar alertas para consola sin alterar el inventario ni enviar notificaciones externas.
Las clases nuevas trabajan con los DAO/servicios de la issue #33.

## Integración

- `PublicadorInventario`: suscribe/desuscribe `ObservadorInventario`, y notifica mediante `publicar(repuestoId)`.
- `LoteService.registrarIngreso`: publica por cada repuesto **después** del `commit`.
- `RepuestoService.crear/actualizar/eliminar`: publica después de `commit` cuando hay un cambio real.
- `AlertasInventarioService`: implementa `ObservadorInventario`, recalcula alertas y brinda `consultarAlertas()`, `ultimasAlertas()` y `reporteConsola()`.
- `AlertasDAOJdbc`: obtiene `repuestos.id`, `nombre` y `stock_minimo` de MySQL. Reutiliza `ExistenciasService` para las cantidades y fechas (no duplica SQL de reservas).
- `AlertasInventarioController`: `mostrarReporte()` para uso posterior en consola.

Ejemplo de armado (en el futuro `Main` o vista):

```java
PublicadorInventario publicador = new PublicadorInventario();
ExistenciasService existencias = new ExistenciasService(
    new ExistenciasDAOJdbc(),
    () -> ConexionFactory.getInstancia().crearConexion(), LocalDate::now);
AlertasInventarioService alertas = new AlertasInventarioService(
    new AlertasDAOJdbc(), existencias,
    () -> ConexionFactory.getInstancia().crearConexion(), LocalDate::now, 7);
publicador.suscribir(alertas);
LoteService lotes = new LoteService(new LoteDAOJdbc(), proveedores,
    () -> ConexionFactory.getInstancia().crearConexion(), LocalDate::now, publicador);
RepuestoService repuestos = new RepuestoService(new RepuestoDAOJdbc(), proveedores,
    () -> ConexionFactory.getInstancia().crearConexion(), publicador);
String reporte = new AlertasInventarioController(alertas).mostrarReporte();
System.out.println(reporte);
```

**Importante:** utilizar **la misma instancia** de `PublicadorInventario` para los servicios emisores y el observador. Los constructores antiguos de los servicios siguen existiendo, pero crean un publicador vacío para compatibilidad.

## Contratos de negocio

- `STOCK_BAJO`: `resumen.disponibleUtilizable < repuestos.stock_minimo`. El disponible utilizable excluye lotes vencidos y resta reservas activas. `stock_minimo = 0` no produce alerta por cero unidades.
- `CADUCIDAD_PROXIMA`: un lote tiene `cantidad_actual > 0`, caducidad no nula y `hoy <= caducidad <= hoy.plusDays(diasAviso)`. Un lote que caducó ayer no cuenta como *próximo* (puede consultarse por separado como vencido si se amplía la issue).
- `diasAviso` configurable en el constructor, valor >= 0. `LocalDate` inyectable (`() -> LocalDate.of(...)`) para pruebas repetibles.
- Unidades enteras (`long`) y fechas `LocalDate`; no se suman precios ni se crean movimientos de consumo.
- `consultarAlertas()` genera un reporte actualizado aunque ningún evento haya ocurrido todavía.
- `ultimasAlertas()` devuelve la última foto calculada por consulta o por evento; es **memoria volátil**, no almacenamiento durable.
- El observador se invoca **después** de un commit exitoso; si la operación hace `rollback`, no ocurre `publicar`.
- Si falla la actualización de una alerta posterior al commit, `PublicadorInventario` informa por `System.err` y continúa con los otros observadores. No se debe informar que el commit falló cuando realmente ya fue confirmado.

## Alcance pendiente de integración

- El módulo de reservas/consumo de órdenes, cuando exista, deberá emitir a **este mismo** publicador *después* de su propio commit. Este módulo **no** escribe ni altera reservas.
- Cambios SQL realizados directamente en MySQL no generan automáticamente eventos Java. Para refrescar, consultar el reporte de nuevo.
- La consulta del reporte usa varios accesos de solo lectura a MySQL, por lo que no es una foto atómica frente a escrituras concurrentes; si se exige consistencia instantánea, habrá que diseñar un snapshot transaccional.
- Prueba unitaria con fixtures de memoria `AlertasInventarioServiceTest`, más `ObserverIntegracionTest` con MySQL disponible. Las pruebas de integración se omiten si MySQL no está accesible.

## Validación local

```bash
mvn -Dtest=AlertasInventarioServiceTest test
mvn -Dtest=ObserverIntegracionTest test
mvn test
```
