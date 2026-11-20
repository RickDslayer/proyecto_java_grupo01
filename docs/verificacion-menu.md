# Verificación del menú de consola

La revisión utiliza `Main.ensamblar`, el Scanner real de las vistas y MySQL local
preparado con `schema.sql` y `data.sql`. No sustituye los controladores, servicios
ni DAO por simulaciones. Los registros de cada escenario se consultan en MySQL
para comprobar el efecto de las opciones de consola y se eliminan al finalizar.

## Ejecución reproducible

```sh
mvn -Dtest=MenuConsolaIntegracionTest,ConsolaTest test
mvn verify
```

Las pruebas de integración se omiten si no hay MySQL disponible. Para obtener una
verificación completa, deben ejecutarse con el servidor y los datos preparados
según el README. No ejecutar `schema.sql` sobre datos que se quieran conservar.

## Cobertura de navegación y operaciones

| Área | Comprobaciones |
|---|---|
| Acceso | Credenciales válidas de administrador/técnico, credenciales inválidas, cerrar sesión y EOF. |
| Navegación | Submenús administrativos y técnicos, regreso con 0, rechazo de opciones inexistentes y fin de entrada. |
| Clientes | Alta, listado, búsqueda por documento/ID, actualización, retiro de correo y eliminación; rechazo al eliminar con vehículos relacionados. |
| Vehículos | Alta por propietario, búsquedas, listados por cliente, actualización y eliminación sin referencias. |
| Técnicos | Alta, búsquedas/listados, actualización a inactivo, eliminación y selección del contexto técnico. |
| Repuestos y tipos | Altas, búsquedas/listados, actualización de precios/ofertas, importes cero, CRUD de tipos y protección de repuestos con lotes. |
| Proveedores | Alta, búsquedas/listados, actualización y protección contra eliminación con relaciones. |
| Ofertas | Consultas por proveedor/repuesto/pareja, nueva asociación y actualización de costo. |
| Evaluaciones | Registro de calidad y puntualidad de un lote, consultas y promedios del proveedor. |
| Inventario | Ingreso real de lote, búsquedas, stock, FEFO, alertas, reserva, ajuste y liberación; rechazo por falta de stock. |
| Órdenes | Alta, búsquedas por ID/vehículo/estado, listado y operaciones de mano de obra por administrador o técnico asignado. |
| Supervisión | Carga En Proceso, técnicos sin órdenes abiertas, consultas, asignación e inicio automático. |
| Tiempo | Rechazo de 2:3; acumulación de 02:30 + 00:45 = 03:15. |
| Trabajos técnicos | Listas activas/historial, detalle, responsables, operaciones y finalización de una orden asignada. |
| Facturación | Consulta, guardado y TXT automático, reexportación, una sola factura por orden y entrega. |
| Historial | Consulta por vehículo/cliente, detalle de operaciones/precios y recordatorios después de la entrega. |
| Reportes | Los tres reportes, sus tres órdenes de presentación, fechas válidas y rechazo de fechas inválidas o periodos invertidos. |
| Cancelación | N, ENTER o EOF no confirman; cancelar una edición no cambia MySQL. |

El recorrido completo registra cliente, vehículo y técnico desde consola;
registra proveedor, repuesto y lote; crea una orden Pendiente; asigna al técnico
y verifica En Proceso; suma horas; registra mano de obra; reserva/ajusta/libera
y vuelve a reservar repuestos; completa desde el menú técnico; comprueba consumo;
factura y verifica el TXT; entrega y consulta historial y recordatorios.

## Correcciones realizadas durante la revisión

- En Mis trabajos se aceptaba la opción 3 sin mostrarla. Ahora ese menú admite
  solamente 1 (activos), 2 (historial) y 0 (regresar). La opción 3 del detalle de
  una orden sí existe y mantiene el acceso a operaciones de mano de obra.
- La confirmación documentaba que ENTER cancelaba, pero lo rechazaba como
  opción inválida. Ahora únicamente S confirma; N, ENTER y EOF cancelan.

## Criterios del proyecto contrastados

- Se conserva MVC: las vistas delegan al controlador, y los servicios gestionan
  las reglas y transacciones. Los DAO nuevos también tienen Factory Method.
- Están presentes Singleton, DAO, Factory Method, Strategy y Observer, además de
  lambdas y Streams para consultas, presentación y reportes.
- Las reservas reducen el disponible; completar descuenta el físico una sola
  vez. Los tests existentes también verifican rollback y facturación concurrente.
- Los precios de la orden alimentan el cobro y el historial. Sus operaciones no
  se pueden modificar una vez completada.
- La factura TXT contiene cliente, vehículo, repuestos, mano de obra e importes.
  Se genera al facturar una orden ya completada, conservando los pasos separados
  Completado → Facturado → Entregado del flujo acordado.
- Un fallo al escribir el TXT no revierte la factura confirmada; se puede
  reexportar. Estos casos se cubren también en ExportadorFacturaTxtTest y
  OperacionFacturacionIntegracionTest.
- Los recordatorios se calculan desde las operaciones entregadas y su intervalo;
  son reportes consultables y no envíos de mensajes externos.
- Los tres indicadores mantienen las métricas documentadas: demanda por tipo de
  mantenimiento, participaciones por técnico y unidades de repuestos consumidas.
- El impuesto usa configuración local en código (19 % de demostración), sin
  variable de entorno. La conexión continúa siendo MySQL local por equipo.
- README, esquema, datos iniciales, diagramas y reglas actuales de `.gitignore`
  están presentes. Las decisiones históricas de ramas y mensajes de commit
  se mantienen según lo acordado con el equipo.

El acceso por roles es demostrativo, con las credenciales descritas en el README;
no representa un módulo adicional de autenticación de empleados en MySQL.
