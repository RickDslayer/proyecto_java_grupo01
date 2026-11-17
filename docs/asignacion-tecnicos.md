# Tarea 06 — Asignación de técnicos a órdenes

Esta rama parte del commit local de la tarea 05. Reutiliza servicios, tecnicos y
servicios_tecnicos del esquema aprobado, sin agregar tablas o IDs artificiales.

## Componentes

- ServicioTecnico: relación identificada por (servicioId, tecnicoId), con horas
  BigDecimal y datos de consulta de técnico/estado.
- ServicioTecnicoDAO/Jdbc: asignar, comprobar existencia, retirar, actualizar
  horas, listar por orden y listar por técnico; PreparedStatement en todas las
  consultas. Las consultas de referencias con FOR UPDATE permiten validar y
  escribir en una misma unidad de trabajo.
- ServicioTecnicoDAOFactory: Factory Method.
- AsignacionTecnicoService: reglas, bloqueos y transacciones.
- AsignacionTecnicoController: adapta texto y devuelve mensajes a la futura vista.

## Reglas

Varios técnicos pueden estar en una orden y un técnico en varias órdenes.
No se imponen exclusividades ni agendas que el enunciado no solicita.

Para asignar, orden y técnico deben existir; el técnico debe estar activo.
No se repite la pareja (servicio, técnico), protegida también por la PK de MySQL.
Solo Pendiente y En Proceso admiten asignar, retirar o cambiar horas; Completado,
Facturado y Entregado conservan sus responsables e historial.

En una asignación existente se pueden retirar técnicos inactivos o registrar
sus horas mientras la orden esté abierta. No se modifica su estado de actividad.
Las horas admiten 0..999.99 con dos decimales como DECIMAL(5,2); registrarHoras
sustituye el total, no acumula automáticamente. Repetir el mismo total es válido.

Las escrituras bloquean primero la orden y después el técnico con SELECT FOR
UPDATE, validan y operan sobre la misma conexión. Commit/rollback y cierre son
responsabilidad del servicio. Los próximos cambios de estado de órdenes deberán
respetar el mismo bloqueo de su fila para coordinarse con estas operaciones.
No se implementan aún transiciones, consumo ni facturación.

## Uso

```java
AsignacionTecnicoService servicio = new AsignacionTecnicoService(
    new ServicioTecnicoDAOFactory(),
    () -> ConexionFactory.getInstancia().crearConexion());
AsignacionTecnicoController controlador = new AsignacionTecnicoController(servicio);
String resultado = controlador.asignar("1", "2");
String horas = controlador.registrarHoras("1", "2", "2.50");
String responsables = controlador.listarPorOrden("1");
```

La vista muestra esos resultados. listarPorTecnico permite consultar las
participaciones/horas que necesitará el módulo de productividad.

## Pruebas

`mvn verify`: cardinalidad N:M, duplicados, inexistentes/inactivos, órdenes
cerradas, horas exactas, consultas, fallos SQL, rollback y mensajes con dobles.
ServicioTecnicoDAOJdbcIntegracionTest verifica FK/PK, consultas y rollback con
MySQL local si está disponible; no recrea la base ni borra fixtures ajenos.
Requiere vehículos/catálogos cargados y estados de la tarea 05 actualizados.
La prueba de MySQL se omite explícitamente si no hay servidor. No se afirma
haber probado concurrencia real solo por pasar pruebas con dobles.
