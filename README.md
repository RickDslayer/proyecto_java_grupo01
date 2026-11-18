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
