package com.carmotors.vista;

import com.carmotors.Main;
import com.carmotors.config.ConexionFactory;
import com.carmotors.config.ConfiguracionAplicacion;
import com.carmotors.excepcion.ConexionException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Recorre el menú real de Main; las comprobaciones consultan MySQL, sin dobles de servicios. */
@Timeout(60)
class MenuConsolaIntegracionTest {
    @TempDir Path carpeta;

    @BeforeAll static void mysqlDisponible() throws Exception {
        try (Connection con = conexion()) { assumeTrue(con.isValid(2), "MySQL local no disponible."); }
        catch (ConexionException e) { assumeTrue(false, "MySQL local no disponible."); }
    }

    @Test void todosLosSubmenusAdministrativosAceptanRegresoEntradaInvalidaYFinDeEntrada() {
        String[][] rutas = {{"1"}, {"2"}, {"3"}, {"3","1"}, {"3","2"}, {"3","3"},
                {"3","4"}, {"3","4","6"}, {"3","5"}, {"3","5","7"}, {"3","5","8"},
                {"4"}, {"5"}, {"6"}, {"7"}};
        for (String[] ruta : rutas) {
            List<String> entrada = new ArrayList<>(List.of(ruta));
            entrada.add("999"); entrada.add("0");
            String salida = admin(entrada.toArray(String[]::new));
            assertTrue(salida.contains("Opción no válida"), String.join("/", ruta));
            sinFallos(salida);
        }
    }

    @Test void consultasDeClientesVehiculosTecnicosYProveedoresDesdeElMenu() throws Exception {
        String cliente = dato("SELECT id FROM clientes ORDER BY id LIMIT 1");
        String documento = dato("SELECT documento_identidad FROM clientes WHERE id=?", cliente);
        String vehiculo = dato("SELECT id FROM vehiculos WHERE cliente_id=? ORDER BY id LIMIT 1", cliente);
        String placa = dato("SELECT placa FROM vehiculos WHERE id=?", vehiculo);
        String tecnico = dato("SELECT id FROM tecnicos ORDER BY id LIMIT 1");
        String docTecnico = dato("SELECT documento_identidad FROM tecnicos WHERE id=?", tecnico);
        for (String[] ruta : new String[][]{{"3","1","2"}, {"3","1","3",documento}, {"3","1","4",cliente},
                {"3","2","2",placa}, {"3","2","3",vehiculo}, {"3","2","4"}, {"3","2","5",documento},
                {"3","3","1"}, {"3","3","2",docTecnico}, {"3","3","3",tecnico},
                {"3","5","1"}, {"3","5","2","a"}, {"3","5","3","1"}}) sinFallos(admin(ruta));
    }

    @Test void consultasDeInventarioOfertasEvaluacionesYReportes() throws Exception {
        String codigo = dato("SELECT codigo FROM lotes ORDER BY id LIMIT 1");
        for (String[] ruta : new String[][]{{"3","4","1"}, {"3","4","2","a"}, {"3","4","6","1"},
                {"4","1"}, {"4","2","1"}, {"4","3"}, {"4","5",codigo}, {"4","6","1"},
                {"4","7","1"}, {"4","8","1","1"}, {"3","5","7","1","1"},
                {"3","5","7","2","1"}, {"3","5","7","3","1","1"},
                {"3","5","8","2","1"}, {"3","5","8","3","1"}, {"3","5","8","4","1"}}) sinFallos(admin(ruta));
        for (String reporte : List.of("1", "2", "3")) {
            for (String orden : List.of("1", "2", "3")) {
                String salida = admin("7", reporte, "2020-01-01", "2030-12-31", orden);
                sinFallos(salida);
                assertFalse(salida.contains("Error:"));
            }
        }
        assertTrue(admin("7","1","fecha-invalida","2030-01-01","1").contains("Error:"));
        assertTrue(admin("7","2","2030-01-01","2020-01-01","1").contains("Error:"));
    }

    @Test void consultasDeOrdenesHistorialYFacturas() throws Exception {
        String orden = dato("SELECT id FROM servicios ORDER BY id LIMIT 1");
        String placa = dato("SELECT v.placa FROM vehiculos v JOIN servicios s ON s.vehiculo_id=v.id WHERE s.id=?", orden);
        String documento = dato("SELECT c.documento_identidad FROM clientes c JOIN vehiculos v ON v.cliente_id=c.id WHERE v.placa=?", placa);
        String facturada = dato("SELECT servicio_id FROM facturas ORDER BY id LIMIT 1");
        for (String[] ruta : new String[][]{{"1","1"}, {"1","2"}, {"1","3","1"}, {"1","4","1"},
                {"2","2",orden}, {"2","3"}, {"2","4",placa}, {"5","1"}, {"5","3"}, {"5","5",orden}, {"5","6"},
                {"5","7",facturada}, {"6","1",placa,"1"}, {"6","2",documento}, {"6","3",placa}, {"6","4",documento}}) sinFallos(admin(ruta));
        for (String estado : List.of("1","2","3","4","5")) sinFallos(admin("2","5",estado));
        assertTrue(admin("5","7","2147483647").contains("no tiene una factura"));
    }

    @Test void loginYMenuTecnicoNoAceptanOpcionesOcultas() {
        String rechazada = ejecutar("1","incorrecto","incorrecta","0");
        assertTrue(rechazada.contains("Credenciales no válidas"));
        String salida = tecnico("1","1","3","0");
        assertTrue(salida.contains("Opción no válida"));
        for (String[] ruta : new String[][]{{"2"}, {"3"}, {"4"}, {"5","0"}, {"6"}, {"7"},
                {"2","3"}, {"3","2"}, {"6","1"}, {"6","2","1"}, {"1","1","0"}, {"1","2","0"}}) sinFallos(tecnico("1", ruta));
    }

    @Test void cancelarYTerminarUnaEntradaNoGuardaCambios() throws Exception {
        try (Fixture f = new Fixture()) {
            f.crearClienteVehiculoTecnico();
            String anterior=dato("SELECT nombre FROM clientes WHERE id=?",f.cliente);
            sinFallos(admin("3","1","5",f.cliente,"Cambio cancelado","","","","N"));
            assertEquals(anterior,dato("SELECT nombre FROM clientes WHERE id=?",f.cliente));
            sinFallos(admin("3","1","5",f.cliente,"Cambio interrumpido","","",""));
            assertEquals(anterior,dato("SELECT nombre FROM clientes WHERE id=?",f.cliente));
            sinFallos(admin("3","1","1","Sin confirmación",f.documento+"X","3001234567","",""));
            assertNull(dato("SELECT id FROM clientes WHERE documento_identidad=?",f.documento+"X"));
        }
    }

    @Test void clientesVehiculosYTecnicosSeRegistranActualizanYEliminanDesdeConsola() throws Exception {
        try (Fixture f = new Fixture()) {
            f.crearClienteVehiculoTecnico();
            sinFallos(admin("3","1","5",f.cliente,"Nombre actualizado","","","-","S"));
            assertEquals("NOMBRE ACTUALIZADO", dato("SELECT nombre FROM clientes WHERE id=?",f.cliente));
            assertNull(dato("SELECT email FROM clientes WHERE id=?", f.cliente));
            sinFallos(admin("3","2","6",f.vehiculo,"","","","S"));
            sinFallos(admin("3","3","5",f.tecnico,"Técnico actualizado","","","","N","S"));
            assertEquals("0",dato("SELECT activo FROM tecnicos WHERE id=?",f.tecnico));
            // La aplicación conserva las referencias: no permite borrar al propietario de un vehículo.
            assertTrue(admin("3","1","6",f.cliente,"S").contains("[!]"));
            assertNotNull(dato("SELECT id FROM clientes WHERE id=?",f.cliente));
            sinFallos(admin("3","2","7",f.vehiculo,"S"));
            assertNull(dato("SELECT id FROM vehiculos WHERE id=?",f.vehiculo));
            sinFallos(admin("3","1","6",f.cliente,"S"));
            sinFallos(admin("3","3","6",f.tecnico,"S"));
            assertNull(dato("SELECT id FROM clientes WHERE id=?",f.cliente));
            assertNull(dato("SELECT id FROM tecnicos WHERE id=?",f.tecnico));
        }
    }

    @Test void proveedoresOfertasYEvaluacionesSeMantienenDesdeElMenu() throws Exception {
        try (Fixture f = new Fixture()) {
            f.crearProveedorRepuestoYLote();
            sinFallos(admin("3","5","5",f.proveedor,"Proveedor actualizado","","","S"));
            assertEquals("Proveedor actualizado", dato("SELECT nombre FROM proveedores WHERE id=?",f.proveedor));
            sinFallos(admin("3","5","7","5",f.proveedor,f.repuesto,"7.25","S"));
            assertEquals("7.25",dato("SELECT costo FROM proveedores_repuestos WHERE proveedor_id=? AND repuesto_id=?",f.proveedor,f.repuesto));
            sinFallos(admin("3","5","7","4",f.proveedor,"1","9.00","S"));
            assertEquals("9.00",dato("SELECT costo FROM proveedores_repuestos WHERE proveedor_id=? AND repuesto_id=1",f.proveedor));
            sinFallos(admin("3","5","8","1",f.proveedor,"1","5","4",LocalDate.now().toString(),"Entrega de prueba","S"));
            assertEquals("5",dato("SELECT calificacion_calidad FROM evaluaciones_proveedor WHERE lote_id=?",f.lote));
            assertTrue(admin("3","5","8","4",f.proveedor).contains("5.00/5"));
            assertTrue(admin("3","5","6",f.proveedor,"S").contains("No se completó"));
            assertNotNull(dato("SELECT id FROM proveedores WHERE id=?",f.proveedor));
            assertTrue(admin("3","4","5",f.repuesto,"S").contains("No se puede eliminar"));
            assertNotNull(dato("SELECT id FROM repuestos WHERE id=?",f.repuesto));
        }
    }

    @Test void tiposYRepuestosSinReferenciasSePuedenEditarYEliminar() throws Exception {
        try (Fixture f = new Fixture()) {
            sinFallos(admin("3","5","4","Proveedor "+f.sufijo,"Contacto","","S"));
            f.proveedor=dato("SELECT id FROM proveedores WHERE nombre=?","Proveedor "+f.sufijo);
            String tipo=null;
            try {
                sinFallos(admin("3","4","6","2","Tipo "+f.sufijo,"S"));
                tipo=dato("SELECT id FROM repuesto_tipos WHERE nombre=?","Tipo "+f.sufijo); assertNotNull(tipo);
                sinFallos(admin("3","4","6","3",tipo,"Tipo actualizado "+f.sufijo,"S"));
                sinFallos(admin("3","4","3",tipo,"1",f.proveedor,"Repuesto "+f.sufijo,"0.00","0.00","0","S"));
                f.repuesto=dato("SELECT id FROM repuestos WHERE nombre=?","Repuesto "+f.sufijo); assertNotNull(f.repuesto);
                sinFallos(admin("3","4","4",f.repuesto,tipo,"1",f.proveedor,"Repuesto actualizado "+f.sufijo,"15.00","25.00","3","S"));
                assertEquals("25.00",dato("SELECT precio_venta FROM repuestos WHERE id=?",f.repuesto));
                sinFallos(admin("3","4","5",f.repuesto,"S"));
                assertNull(dato("SELECT id FROM repuestos WHERE id=?",f.repuesto));
                sinFallos(admin("3","4","6","4",tipo,"S"));
                assertNull(dato("SELECT id FROM repuesto_tipos WHERE id=?",tipo));
                sinFallos(admin("3","5","6",f.proveedor,"S"));
                assertNull(dato("SELECT id FROM proveedores WHERE id=?",f.proveedor));
            } finally { sql("DELETE FROM repuesto_tipos WHERE id=?",tipo); }
        }
    }

    @Test void flujoCompletoPorConsolaDesdeAltasHastaEntregaYRecordatorios() throws Exception {
        try (Fixture f = new Fixture()) {
            f.crearClienteVehiculoTecnico();
            f.crearProveedorRepuestoYLote();
            sinFallos(admin("2","1",f.placa,"1","Trabajo de prueba por consola","S"));
            f.orden = dato("SELECT id FROM servicios WHERE vehiculo_id=?", f.vehiculo);
            assertEquals("Pendiente", estado(f.orden));
            String posicionOrden = posicion("SELECT id FROM servicios ORDER BY fecha_recepcion,id", f.orden);
            String posicionTecnico = posicion("SELECT id FROM tecnicos WHERE activo=1 ORDER BY id", f.tecnico);
            sinFallos(admin("1","5",posicionOrden,posicionTecnico,"S"));
            assertEquals("En Proceso",estado(f.orden));
            assertTrue(admin("1","5",posicionOrden,posicionTecnico).contains("ya tiene asignada"));
            assertTrue(admin("1","6",posicionOrden,"1","2:3","S").contains("HH:mm"));
            assertTrue(admin("1","6",posicionOrden,"1","02:30","S").contains("Nuevo acumulado: 02:30"));
            assertTrue(admin("1","6",posicionOrden,"1","00:45","S").contains("Nuevo acumulado: 03:15"));
            String operacion = dato("SELECT id FROM servicios_catalogo WHERE intervalo_meses IS NOT NULL ORDER BY id LIMIT 1");
            sinFallos(admin("2","6",f.orden,"3",operacion,"100.00","S"));
            assertEquals("100.00",dato("SELECT precio_cobrado FROM servicios_detalle WHERE servicio_id=?",f.orden));
            assertTrue(admin("4","9",f.orden,f.repuesto,"999","S").contains("Error:"));
            assertEquals("0",dato("SELECT COUNT(*) FROM servicios_repuestos WHERE servicio_id=?",f.orden));
            sinFallos(admin("4","9",f.orden,f.repuesto,"2","S"));
            assertEquals("10",dato("SELECT cantidad_actual FROM lotes_repuestos WHERE lote_id=?",f.lote));
            sinFallos(admin("4","10",f.orden,f.repuesto,"3","S"));
            assertEquals("3",dato("SELECT SUM(cantidad) FROM reservas_repuestos WHERE servicio_id=? AND estado_id=(SELECT id FROM reserva_estados WHERE nombre='ACTIVA')",f.orden));
            sinFallos(admin("4","11",f.orden,f.repuesto,"S"));
            assertEquals("0",dato("SELECT COUNT(*) FROM reservas_repuestos WHERE servicio_id=? AND estado_id=(SELECT id FROM reserva_estados WHERE nombre='ACTIVA')",f.orden));
            // Una línea liberada debe reservarse otra vez antes de completar.
            sinFallos(admin("4","10",f.orden,f.repuesto,"2","S"));
            String operacionesTecnico=tecnico(posicionTecnico,"1","1","1","3","2");
            sinFallos(operacionesTecnico);
            assertTrue(operacionesTecnico.contains("100.00"));
            sinFallos(tecnico(posicionTecnico,"1","1","1","2","S"));
            assertEquals("Completado",estado(f.orden));
            assertEquals("8",dato("SELECT cantidad_actual FROM lotes_repuestos WHERE lote_id=?",f.lote));
            assertTrue(admin("2","6",f.orden,"4",operacion,"1.00","S").contains("Solo se pueden modificar"));
            sinFallos(admin("5","2",f.orden,"S"));
            assertEquals("Facturado",estado(f.orden));
            f.factura = dato("SELECT id FROM facturas WHERE servicio_id=?",f.orden);
            Path txt = ConfiguracionAplicacion.CARPETA_FACTURAS.resolve("factura-"+f.factura+"-orden-"+f.orden+".txt");
            String contenido = Files.readString(txt);
            assertTrue(contenido.contains(f.placa));
            assertTrue(contenido.contains("TOTAL: 166.60"));
            assertTrue(admin("5","2",f.orden,"S").contains("Transición no permitida"));
            assertEquals("1",dato("SELECT COUNT(*) FROM facturas WHERE servicio_id=?",f.orden));
            sinFallos(admin("5","8",f.orden,carpeta.toString()));
            sinFallos(admin("5","4",f.orden,"S"));
            assertEquals("Entregado",estado(f.orden));
            assertTrue(admin("6","1",f.placa,"1").contains("100.00"));
            assertTrue(admin("6","3",f.placa).contains("próxima fecha:"));
            sinFallos(admin("6","4",f.documento));
            for (String reporte : List.of("1","2","3")) sinFallos(admin("7",reporte,LocalDate.now().toString(),LocalDate.now().toString(),"1"));
        }
    }

    private static String admin(String... ruta) { return ejecutar(unir(new String[]{"1","admin","demo123"},ruta)); }
    private static String tecnico(String seleccion, String... ruta) { return ejecutar(unir(new String[]{"2","tecnico","demo123",seleccion},ruta)); }
    private static String[] unir(String[] inicio, String[] fin) {
        List<String> todo = new ArrayList<>(List.of(inicio)); todo.addAll(List.of(fin)); return todo.toArray(String[]::new);
    }
    private static String ejecutar(String... entrada) {
        var salida = new ByteArrayOutputStream();
        Main.ensamblar(new Consola(new Scanner(String.join("\n",entrada)+"\n"),new PrintStream(salida,true,StandardCharsets.UTF_8))).iniciar();
        String texto = salida.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("Hasta pronto"),texto);
        return texto;
    }
    private static void sinFallos(String salida) {
        for (String error : List.of("Error:","Error del sistema:","No se pudo", "No se completó", "[!] Validación")) {
            assertFalse(salida.contains(error),salida);
        }
    }
    private static Connection conexion() { return ConexionFactory.getInstancia().crearConexion(); }
    private static String dato(String sql, Object... parametros) throws Exception {
        try (Connection con=conexion(); var ps=con.prepareStatement(sql)) {
            for(int i=0;i<parametros.length;i++) ps.setObject(i+1,parametros[i]);
            try(var rs=ps.executeQuery()) { return rs.next()?rs.getString(1):null; }
        }
    }
    private static void sql(String consulta, Object... parametros) throws Exception {
        try(Connection con=conexion();var ps=con.prepareStatement(consulta)) {
            for(int i=0;i<parametros.length;i++) ps.setObject(i+1,parametros[i]); ps.executeUpdate();
        }
    }
    private static String posicion(String consulta,String id) throws Exception {
        try(Connection con=conexion();var ps=con.prepareStatement(consulta);var rs=ps.executeQuery()) {
            int posicion=0; while(rs.next()) { posicion++; if(rs.getString(1).equals(id))return Integer.toString(posicion); }
        }
        throw new AssertionError("No está el ID "+id+" en "+consulta);
    }
    private static String estado(String orden) throws Exception {
        return dato("SELECT e.nombre FROM servicios s JOIN servicio_estados e ON e.id=s.estado_id WHERE s.id=?",orden);
    }

    private static final class Fixture implements AutoCloseable {
        final String sufijo=Long.toUnsignedString(System.nanoTime(),36);
        final String documento="MC"+sufijo;
        final String placa=("M"+sufijo.substring(Math.max(0,sufijo.length()-9))).toUpperCase(java.util.Locale.ROOT);
        String cliente,vehiculo,tecnico,proveedor,repuesto,lote,orden,factura;
        void crearClienteVehiculoTecnico() throws Exception {
            sinFallos(admin("3","1","1","Cliente consola",documento,"3001234567",documento+"@example.com","S"));
            cliente=dato("SELECT id FROM clientes WHERE documento_identidad=?",documento); assertNotNull(cliente);
            sinFallos(admin("3","2","1",placa,documento,"1","S"));
            vehiculo=dato("SELECT id FROM vehiculos WHERE placa=?",placa); assertNotNull(vehiculo);
            sinFallos(admin("3","3","4","Técnico consola",documento,"3001234567","Mecánica","S"));
            tecnico=dato("SELECT id FROM tecnicos WHERE documento_identidad=?",documento); assertNotNull(tecnico);
        }
        void crearProveedorRepuestoYLote() throws Exception {
            sinFallos(admin("3","5","4","Proveedor "+sufijo,"Contacto de prueba",sufijo+"@example.com","S"));
            proveedor=dato("SELECT id FROM proveedores WHERE nombre=?","Proveedor "+sufijo); assertNotNull(proveedor);
            sinFallos(admin("3","4","3","1","1",proveedor,"Repuesto "+sufijo,"10.00","20.00","2","S"));
            repuesto=dato("SELECT id FROM repuestos WHERE nombre=?","Repuesto "+sufijo); assertNotNull(repuesto);
            sinFallos(admin("4","4",proveedor,"LOTE-"+sufijo,"",repuesto,"10","10.00",LocalDate.now().plusMonths(12).toString(),"S"));
            lote=dato("SELECT id FROM lotes WHERE codigo=?","LOTE-"+sufijo); assertNotNull(lote);
        }
        @Override public void close() throws Exception {
            if(factura==null && orden!=null) factura=dato("SELECT id FROM facturas WHERE servicio_id=?",orden);
            if(factura!=null) Files.deleteIfExists(ConfiguracionAplicacion.CARPETA_FACTURAS.resolve("factura-"+factura+"-orden-"+orden+".txt"));
            for(String tabla:List.of("facturas","reservas_repuestos","servicios_repuestos","servicios_detalle","servicios_tecnicos")) sql("DELETE FROM "+tabla+" WHERE servicio_id=?",orden);
            sql("DELETE FROM servicios WHERE id=?",orden);
            sql("DELETE FROM evaluaciones_proveedor WHERE lote_id=?",lote);
            sql("DELETE FROM lotes_repuestos WHERE lote_id=?",lote); sql("DELETE FROM lotes WHERE id=?",lote);
            sql("DELETE FROM proveedores_repuestos WHERE repuesto_id=? OR proveedor_id=?",repuesto,proveedor); sql("DELETE FROM repuestos WHERE id=?",repuesto);
            sql("DELETE FROM proveedores WHERE id=?",proveedor); sql("DELETE FROM tecnicos WHERE id=?",tecnico);
            sql("DELETE FROM vehiculos WHERE id=?",vehiculo); sql("DELETE FROM clientes WHERE id=?",cliente);
        }
    }
}
