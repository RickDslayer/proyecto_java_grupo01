# proyecto_java_grupo01

## Reporte de servicios más solicitados

Con [MySQL local configurado](docs/configuracion-jdbc.md) y los datos de
`database/data.sql` cargados, ejecuta desde la raíz del proyecto:

```sh
mvn compile exec:java -Dexec.mainClass=com.carmotors.vista.ReporteServiciosConsola -Dexec.args="2026-11-01 2026-11-30"
```

El reporte cuenta las órdenes por tipo de mantenimiento según su fecha de
recepción. Ambas fechas se incluyen y se cuentan las órdenes de cualquier
estado. Por defecto se muestran primero los tipos con más solicitudes.
Como tercer argumento se puede indicar `CANTIDAD_DESC`, `CANTIDAD_ASC` o
`NOMBRE_ASC`.

El catálogo inicial distingue mantenimiento preventivo y correctivo. Cualquier
ampliación a servicios específicos del catálogo requiere un acuerdo del equipo.
Las cantidades reflejan demanda registrada. El reporte no calcula rentabilidad
porque no utiliza ingresos ni costos.

Para verificar la consulta JDBC y el reporte con un fixture independiente en
memoria (H2 en modo MySQL), ejecuta
`mvn -Dtest=ServiciosMasSolicitadosStrategyTest test`.

## Reporte de productividad por técnico

`ProductividadTecnicosStrategy` implementa `ReporteStrategy` con el tipo
`PRODUCTIVIDAD_TECNICOS`. Para integrarla con el módulo de reportes:

```java
ReporteStrategy estrategia = new ProductividadTecnicosStrategy();
ReporteDTO reporte = estrategia.generar(new FiltroReporte(
        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), OrdenReporte.CANTIDAD_DESC));
```

Se usa la fecha de completado y se incluyen ambos días del periodo. Las órdenes
facturadas o entregadas conservan su participación en el periodo en que se
completaron. Se excluyen las órdenes pendientes, en proceso o sin fecha de
completado. Cada técnico asignado suma una participación por orden. Una orden
con dos técnicos suma una participación a cada uno, por lo que la suma puede
superar el total de órdenes completadas. La métrica cuenta asignaciones
registradas, incluidas las de técnicos inactivos, e incluye al técnico aunque
su asignación tenga cero horas registradas.

El fixture propio se verifica con `mvn -Dtest=ProductividadTecnicosStrategyTest test`.
