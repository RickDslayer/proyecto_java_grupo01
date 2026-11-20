package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.config.ConfiguracionAplicacion;
import com.carmotors.dao.factory.*;
import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.ExportacionException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.OrdenServicio;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OperacionFacturacionIntegracionTest {
    @TempDir Path carpeta;

    @Test void operacionesRealesAlimentanFacturaTxtHistorialYRecordatorios() throws Exception {
        Supplier<Connection> conexiones = ConexionFactory.getInstancia()::crearConexion;
        try (Connection con = conexiones.get()) { assumeTrue(con.isValid(2), "MySQL no disponible."); }
        catch (ConexionException e) { assumeTrue(false, "MySQL no disponible."); return; }
        var ordenDao = new OrdenServicioDAOFactory().obtenerDAO();
        var catalogo = new ServicioCatalogoDAOFactory().obtenerDAO();
        int vehiculo;
        int operacion;
        int intervalo;
        try (Connection con = conexiones.get()) {
            try (var ps = con.prepareStatement("SELECT id FROM vehiculos ORDER BY id LIMIT 1"); var rs = ps.executeQuery()) {
                assumeTrue(rs.next(), "Cargar data.sql."); vehiculo = rs.getInt(1);
            }
            var preventiva = catalogo.listar(con).stream().filter(c -> c.intervaloMeses() != null).findFirst();
            assumeTrue(preventiva.isPresent(), "Cargar catálogo preventivo.");
            operacion = preventiva.orElseThrow().id(); intervalo = preventiva.orElseThrow().intervaloMeses();
        }
        OrdenServicio nueva = new OrdenServicio();
        nueva.setVehiculoId(vehiculo); nueva.setTipoMantenimiento("Preventivo"); nueva.setEstado("Pendiente");
        nueva.setFechaRecepcion(LocalDateTime.now().minusMinutes(1)); nueva.setDescripcionProblema("Prueba operaciones/factura");
        int orden;
        try (Connection con = conexiones.get()) { orden = ordenDao.crear(con, nueva); }
        var operaciones = new OperacionOrdenService(new OperacionOrdenDAOFactory().obtenerDAO(), catalogo, ordenDao, conexiones);
        var publicador = new PublicadorInventario();
        var reservas = new ReservasOrdenService(ordenDao, new InventarioReservasFactory().obtenerDAO(), conexiones, Clock.systemDefaultZone(), publicador);
        var ordenes = new OrdenServicioService(ordenDao,
                new VehiculoConsultaImpl(new VehiculoDAOFactory().obtenerDAO()), conexiones, Clock.systemDefaultZone(),
                reservas, new FacturacionService(ConfiguracionAplicacion.TASA_IMPUESTO), publicador);
        var consulta = new ConsultaFacturaService(new FacturaDAOFactory().obtenerDAO(), conexiones, new ExportadorFacturaTxt());
        try {
            assertThrows(ValidacionException.class, () -> consulta.exportar(orden, carpeta));
            assertThrows(ValidacionException.class, () -> operaciones.registrar(orden, operacion, new BigDecimal("-1")));
            assertThrows(ValidacionException.class, () -> operaciones.registrar(orden, operacion, new BigDecimal("1.001")));
            assertThrows(ValidacionException.class, () -> operaciones.registrar(orden, Integer.MAX_VALUE, BigDecimal.ONE));
            assertTrue(operaciones.listar(orden).isEmpty());
            var conFallo = new OperacionOrdenService(new OperacionOrdenDAOFactory().obtenerDAO(), catalogo, ordenDao, () -> {
                Connection real = conexiones.get();
                return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (p, metodo, args) -> {
                    if (metodo.getName().equals("commit")) throw new java.sql.SQLException("Fallo al confirmar operación");
                    try { return metodo.invoke(real, args); }
                    catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                });
            });
            assertThrows(com.carmotors.excepcion.DaoException.class, () -> conFallo.registrar(orden, operacion, new BigDecimal("50.00")));
            assertTrue(operaciones.listar(orden).isEmpty());
            var salidaOperacion = new java.io.ByteArrayOutputStream();
            new com.carmotors.vista.VistaOperaciones(
                    new com.carmotors.vista.Consola(new java.util.Scanner("3\n" + operacion + "\n50.00\nS\n\n0\n"), new java.io.PrintStream(salidaOperacion)),
                    new com.carmotors.controlador.OperacionOrdenController(operaciones)).mostrar(orden);
            assertEquals(new BigDecimal("50.00"), operaciones.listar(orden).get(0).precioCobrado());
            assertThrows(ValidacionException.class, () -> operaciones.registrar(orden, operacion, BigDecimal.ONE));
            operaciones.actualizar(orden, operacion, new BigDecimal("75.00"));
            operaciones.actualizar(orden, operacion, new BigDecimal("75.00"));
            assertEquals(new BigDecimal("75.00"), operaciones.listar(orden).get(0).precioCobrado());
            operaciones.quitar(orden, operacion);
            assertTrue(operaciones.listar(orden).isEmpty());
            assertThrows(ValidacionException.class, () -> operaciones.quitar(orden, operacion));
            assertThrows(ValidacionException.class, () -> operaciones.actualizar(orden, operacion, BigDecimal.ZERO));
            operaciones.registrar(orden, operacion, new BigDecimal("100.00"));
            ordenes.iniciarOrden(orden);
            ordenes.completarOrden(orden);
            assertThrows(ValidacionException.class, () -> operaciones.actualizar(orden, operacion, BigDecimal.ZERO));
            assertThrows(ValidacionException.class, () -> operaciones.quitar(orden, operacion));
            assertThrows(ValidacionException.class, () -> operaciones.registrar(orden, operacion, BigDecimal.ONE));
            var salida = new java.io.ByteArrayOutputStream();
            var consola = new com.carmotors.vista.Consola(new java.util.Scanner("2\n" + orden + "\nS\n\n0\n"),
                    new java.io.PrintStream(salida, true, java.nio.charset.StandardCharsets.UTF_8));
            new com.carmotors.vista.VistaFacturacion(consola,
                    new com.carmotors.controlador.OrdenServicioController(ordenes),
                    new com.carmotors.controlador.FacturaController(consulta), carpeta.resolve("automatica").toString())
                    .mostrar(new com.carmotors.vista.SesionConsola(com.carmotors.vista.SesionConsola.Rol.ADMINISTRADOR, "admin", null));
            assertTrue(salida.toString(java.nio.charset.StandardCharsets.UTF_8).contains("Factura TXT guardada en:"));
            try (var archivos = Files.list(carpeta.resolve("automatica"))) { assertEquals(1, archivos.count()); }
            var documento = consulta.consultar(orden);
            assertEquals(new BigDecimal("100.00"), documento.totalManoObra());
            assertEquals(new BigDecimal("19.00"), documento.impuestos());
            assertEquals(new BigDecimal("119.00"), documento.totalFinal());
            assertFalse(documento.cliente().isBlank()); assertFalse(documento.placa().isBlank());
            Path archivo = consulta.exportar(orden, carpeta.resolve("facturas"));
            assertTrue(Files.readString(archivo).contains("TOTAL: 119.00"));
            assertThrows(ExportacionException.class, () -> consulta.exportar(orden, carpeta.resolve("facturas")));
            Path archivoComoCarpeta = Files.writeString(carpeta.resolve("no-carpeta"), "x");
            assertThrows(ExportacionException.class, () -> consulta.exportar(orden, archivoComoCarpeta));
            assertEquals(documento.id(), consulta.consultar(orden).id());
            assertEquals("Facturado", ordenes.buscarPorId(orden).orElseThrow().getEstado());
            assertTrue(Files.exists(consulta.exportar(orden, carpeta.resolve("reintento"))));
            ordenes.entregarOrden(orden);
            var historial = new HistorialService(new VehiculoService(new VehiculoDAOFactory().obtenerDAO(), new ClienteDAOFactory().obtenerDAO()),
                    new HistorialServiciosConsultaJdbc(new HistorialServiciosDAOFactory().obtenerDAO(), conexiones));
            assertTrue(historial.listarPorVehiculo(vehiculo).stream().anyMatch(s -> s.getServicioId() == orden && s.getOperaciones().size() == 1));
            assertTrue(new RecordatorioPreventivoService(historial, Clock.systemDefaultZone()).listarPorVehiculo(vehiculo).stream()
                    .anyMatch(r -> r.getOperacionId() == operacion && r.getFechaProxima().equals(LocalDate.now().plusMonths(intervalo))));
        } finally {
            try (Connection con = conexiones.get()) {
                for (String tabla : new String[]{"facturas", "servicios_detalle"}) {
                    try (var ps = con.prepareStatement("DELETE FROM " + tabla + " WHERE servicio_id = ?")) { ps.setInt(1, orden); ps.executeUpdate(); }
                }
                try (var ps = con.prepareStatement("DELETE FROM servicios WHERE id = ?")) { ps.setInt(1, orden); ps.executeUpdate(); }
            }
        }
    }
}
