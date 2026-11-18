package com.carmotors;

import com.carmotors.config.ConexionFactory;
import com.carmotors.controlador.EvaluacionProveedorController;
import com.carmotors.dao.EvaluacionProveedorDAO;
import com.carmotors.dao.EvaluacionProveedorDAOJdbc;
import com.carmotors.dao.factory.EvaluacionProveedorDAOFactory;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.EvaluacionProveedor;
import com.carmotors.servicio.EvaluacionProveedorService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Pruebas automatizadas del issue #42, ejecutables desde IntelliJ con main().
 * No requieren JUnit ni el comando mvn.
 *
 * SEGURIDAD: aborta si ConexionFactory no apunta a carmotors_test.
 * Crea dos proveedores y un lote TEMPORALES en una sola transaccion.
 * Elimina los registros temporales en finally, incluso ante fallos.
 * No modifica los datos sembrados por database/data.sql.
 *
 * NOTA: los IDs autoincrementales consumidos pueden dejar saltos aunque
 * las transacciones se reviertan. Esto es normal en MySQL.
 */
public final class EvaluacionProveedorTest {

    private static final String BASE_PERMITIDA = "carmotors_test";
    private static final LocalDate FECHA_1 = LocalDate.of(2026, 11, 10);
    private static final LocalDate FECHA_2 = LocalDate.of(2026, 11, 11);
    private static int verificaciones;

    private EvaluacionProveedorTest() { }

    public static void main(String[] args) throws Exception {
        System.out.println("=== CARMOTORS | ISSUE #42 | PRUEBAS AUTOMATIZADAS ===");

        // Se valida antes de ejecutar cualquier INSERT, UPDATE o DELETE.
        try (Connection conexion = conexionSegura()) {
            verificar(BASE_PERMITIDA.equalsIgnoreCase(conexion.getCatalog()),
                    "La conexion utiliza exclusivamente carmotors_test");
        }

        probarModelo();
        probarFactoryMethod();
        probarValidacionesController();

        Map<String, Huella> historicoAntes = leerHuellaHistorica();
        DatosTemporales datos = null;
        Throwable falloPrincipal = null;

        try {
            datos = crearDatosTemporales();
            probarIntegracionJdbc(datos);
            probarRollbackReal(datos);
            verificar(historicoAntes.equals(leerHuellaHistorica()),
                    "El modulo no altera costos de lotes, ventas ni facturas");
        } catch (Exception | Error e) {
            falloPrincipal = e;
            throw e;
        } finally {
            if (datos != null) {
                try {
                    eliminarDatosTemporales(datos);
                    System.out.println("[LIMPIEZA] Registros temporales eliminados.");
                } catch (Exception | Error errorLimpieza) {
                    if (falloPrincipal != null) {
                        falloPrincipal.addSuppressed(errorLimpieza);
                    } else {
                        throw errorLimpieza;
                    }
                }
            }
        }

        verificar(historicoAntes.equals(leerHuellaHistorica()),
                "Integridad historica conservada despues de la limpieza");
        System.out.println("\nTOTAL DE VERIFICACIONES SUPERADAS: " + verificaciones);
        System.out.println("RESULTADO: PRUEBAS FINALIZADAS CORRECTAMENTE");
    }

    // ---------------------------------------------------------------------
    // 1. MODELO Y FACTORY METHOD
    // ---------------------------------------------------------------------

    private static void probarModelo() {
        System.out.println("\n--- MODELO ---");
        EvaluacionProveedor nueva = new EvaluacionProveedor(
                2, 6, 5, 1, FECHA_1, "  Todo correcto  ");

        verificar(nueva.getId() == 0, "Una evaluacion nueva no tiene ID persistido");
        verificar(nueva.getProveedorId() == 2 && nueva.getLoteId() == 6,
                "Conserva los identificadores de proveedor y lote");
        verificar(nueva.getCalificacionCalidad() == 5
                        && nueva.getCalificacionPuntualidad() == 1,
                "Acepta los limites 1 y 5 de la escala");
        verificar("Todo correcto".equals(nueva.getObservaciones()),
                "Normaliza observaciones con strip()");
        verificar(new EvaluacionProveedor(2, 6, 3, 3, FECHA_1, " ")
                        .getObservaciones() == null,
                "Convierte observaciones en blanco a null");
        verificar(new EvaluacionProveedor(2, 6, 3, 3, FECHA_1, null)
                        .getObservaciones() == null,
                "Permite observaciones nulas");

        esperar(IllegalArgumentException.class,
                () -> new EvaluacionProveedor(0, 6, 4, 4, FECHA_1, null),
                "Rechaza ID de proveedor invalido");
        esperar(IllegalArgumentException.class,
                () -> new EvaluacionProveedor(2, -1, 4, 4, FECHA_1, null),
                "Rechaza ID de lote invalido");
        esperar(IllegalArgumentException.class,
                () -> new EvaluacionProveedor(2, 6, 0, 4, FECHA_1, null),
                "Rechaza calidad menor que 1");
        esperar(IllegalArgumentException.class,
                () -> new EvaluacionProveedor(2, 6, 6, 4, FECHA_1, null),
                "Rechaza calidad mayor que 5");
        esperar(IllegalArgumentException.class,
                () -> new EvaluacionProveedor(2, 6, 4, 0, FECHA_1, null),
                "Rechaza puntualidad menor que 1");
        esperar(IllegalArgumentException.class,
                () -> new EvaluacionProveedor(2, 6, 4, 6, FECHA_1, null),
                "Rechaza puntualidad mayor que 5");
        esperar(NullPointerException.class,
                () -> new EvaluacionProveedor(2, 6, 4, 4, null, null),
                "Rechaza fecha nula");
        esperar(IllegalArgumentException.class,
                () -> EvaluacionProveedor.desdePersistencia(
                        0, 2, 6, 4, 4, FECHA_1, null),
                "Una evaluacion persistida exige ID positivo");

        EvaluacionProveedor persistida = EvaluacionProveedor.desdePersistencia(
                27, 2, 6, 4, 3, FECHA_2, null);
        verificar(persistida.getId() == 27 && FECHA_2.equals(persistida.getFechaEvaluacion()),
                "Reconstruye una evaluacion persistida sin modificarla");
    }

    private static void probarFactoryMethod() {
        System.out.println("\n--- FACTORY METHOD ---");
        EvaluacionProveedorDAOFactory fabrica = new EvaluacionProveedorDAOFactory();
        EvaluacionProveedorDAO dao1 = fabrica.obtenerDAO();
        EvaluacionProveedorDAO dao2 = fabrica.obtenerDAO();
        verificar(dao1 instanceof EvaluacionProveedorDAOJdbc,
                "Factory Method devuelve EvaluacionProveedorDAOJdbc");
        verificar(dao1 != dao2, "Cada llamada obtiene una instancia independiente");
    }

    // ---------------------------------------------------------------------
    // 2. VALIDACIONES DEL CONTROLLER (SIN ESCRITURAS)
    // ---------------------------------------------------------------------

    private static void probarValidacionesController() {
        System.out.println("\n--- CONTROLLER ---");
        EvaluacionProveedorController controller = nuevoController();

        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(2, 6, "abc", "4", "2026-11-10", null),
                "Rechaza calificacion no numerica");
        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(2, 6, "4.5", "4", "2026-11-10", null),
                "Rechaza calificacion no entera");
        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(2, 6, "", "4", "2026-11-10", null),
                "Rechaza calidad vacia");
        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(2, 6, "4", null, "2026-11-10", null),
                "Rechaza puntualidad nula");
        esperar(IllegalArgumentException.class,
                () -> controller.registrarEvaluacion(2, 6, "6", "4", "2026-11-10", null),
                "Valida el rango numerico tambien desde el controller");
        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(2, 6, "4", "4", "10/11/2026", null),
                "Rechaza fecha con formato incorrecto");
        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(2, 6, "4", "4", "2026-02-30", null),
                "Rechaza una fecha inexistente");
        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(2, 6, "4", "4", null, null),
                "Rechaza fecha nula");
        esperar(ValidacionException.class,
                () -> controller.listarPorProveedor(0),
                "Rechaza consultas con proveedor ID cero");
        esperar(ValidacionException.class,
                () -> controller.buscarEvaluacionPorId(-1),
                "Rechaza consultas con evaluacion ID negativo");
    }

    // ---------------------------------------------------------------------
    // 3. INTEGRACION REAL CON JDBC/MYSQL
    // ---------------------------------------------------------------------

    private static void probarIntegracionJdbc(DatosTemporales d) throws SQLException {
        System.out.println("\n--- MYSQL / DAO / SERVICE / CONTROLLER ---");
        EvaluacionProveedorDAO jdbc = new EvaluacionProveedorDAOJdbc();
        EvaluacionProveedorController controller = nuevoController();
        EvaluacionProveedorService service = nuevoService();

        try (Connection conexion = conexionSegura()) {
            verificar(jdbc.lotePerteneceAProveedor(conexion, d.loteId, d.proveedorA),
                    "DAO detecta el proveedor correcto del lote");
            verificar(!jdbc.lotePerteneceAProveedor(conexion, d.loteId, d.proveedorB),
                    "DAO rechaza relacion lote-proveedor incorrecta");
            verificar(!conexion.isClosed(), "DAO no cierra la conexion recibida");
        }

        verificar(controller.listarPorProveedor(d.proveedorB).isEmpty(),
                "Proveedor sin evaluaciones devuelve lista vacia");
        verificar(controller.promedioCalidad(d.proveedorB).isEmpty()
                        && controller.promedioPuntualidad(d.proveedorB).isEmpty(),
                "Proveedor sin evaluaciones no recibe una nota inventada");

        EvaluacionProveedor primera = controller.registrarEvaluacion(
                d.proveedorA, d.loteId, " 5 ", " 4 ",
                FECHA_1.toString(), "  Primera evaluacion  ");
        verificar(primera.getId() > 0, "INSERT devuelve ID autogenerado");
        verificar(primera.getProveedorId() == d.proveedorA
                        && primera.getLoteId() == d.loteId,
                "La evaluacion guardada conserva proveedor y lote");
        verificar("Primera evaluacion".equals(primera.getObservaciones()),
                "Controller y modelo normalizan la observacion");

        EvaluacionProveedor recuperada = controller.buscarEvaluacionPorId(primera.getId())
                .orElseThrow(() -> new AssertionError("No existe el registro guardado"));
        verificar(recuperada.getCalificacionCalidad() == 5
                        && recuperada.getCalificacionPuntualidad() == 4,
                "READ recupera las calificaciones almacenadas en MySQL");
        verificar(FECHA_1.equals(recuperada.getFechaEvaluacion()),
                "READ conserva la fecha sin perdida de informacion");

        // El esquema NO tiene UNIQUE(proveedor_id, lote_id): se admiten
        // evaluaciones multiples para el mismo lote. No es un error del DAO.
        EvaluacionProveedor segunda = controller.registrarEvaluacion(
                d.proveedorA, d.loteId, "3", "2",
                FECHA_2.toString(), null);
        verificar(segunda.getId() != primera.getId(),
                "MySQL permite evaluaciones multiples del mismo lote");
        verificar(segunda.getObservaciones() == null,
                "SQL permite observaciones NULL");

        List<EvaluacionProveedor> lista = controller.listarPorProveedor(d.proveedorA);
        verificar(lista.size() == 2, "Consulta por proveedor recupera las dos evaluaciones");
        verificar(lista.get(0).getId() == segunda.getId()
                        && lista.get(1).getId() == primera.getId(),
                "Listado ordenado por fecha descendente");

        OptionalDouble calidad = controller.promedioCalidad(d.proveedorA);
        OptionalDouble puntualidad = controller.promedioPuntualidad(d.proveedorA);
        verificar(calidad.isPresent() && Math.abs(calidad.getAsDouble() - 4.0) < 1e-9,
                "Streams calcula promedio de calidad (5 + 3) / 2 = 4");
        verificar(puntualidad.isPresent() && Math.abs(puntualidad.getAsDouble() - 3.0) < 1e-9,
                "Streams calcula promedio de puntualidad (4 + 2) / 2 = 3");

        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(
                        d.proveedorB, d.loteId, "5", "5", FECHA_1.toString(), null),
                "Service rechaza un lote de otro proveedor");
        verificar(contarEvaluaciones(d.proveedorB) == 0,
                "El intento rechazado no crea registros para otro proveedor");

        esperar(ValidacionException.class,
                () -> controller.registrarEvaluacion(
                        d.proveedorA, Integer.MAX_VALUE, "5", "5",
                        FECHA_1.toString(), null),
                "Service rechaza un lote inexistente");
        esperar(ValidacionException.class,
                () -> service.registrar(primera),
                "Service rechaza volver a registrar un objeto ya persistido");
        verificar(controller.buscarEvaluacionPorId(Integer.MAX_VALUE).isEmpty(),
                "Busqueda de ID inexistente devuelve Optional.empty()");

        // Verifica la clave foranea compuesta sin pasar por el Service.
        try (Connection conexion = conexionSegura()) {
            conexion.setAutoCommit(false);
            try {
                try {
                    jdbc.insertar(conexion, new EvaluacionProveedor(
                            d.proveedorB, d.loteId, 5, 5, FECHA_1, null));
                    throw new AssertionError("MySQL acepto un lote de otro proveedor");
                } catch (AccesoDatosException error) {
                    verificar(error.getCause() instanceof SQLException sql
                                    && sql.getErrorCode() == 1452,
                            "La FK compuesta de MySQL protege la relacion proveedor-lote");
                }
            } finally {
                conexion.rollback();
            }
        }
    }

    // ---------------------------------------------------------------------
    // 4. PRUEBAS DE ROLLBACK REALES
    // ---------------------------------------------------------------------

    private static void probarRollbackReal(DatosTemporales d) throws SQLException {
        System.out.println("\n--- TRANSACCIONES Y ROLLBACK ---");
        EvaluacionProveedorDAOJdbc jdbc = new EvaluacionProveedorDAOJdbc();
        EvaluacionProveedorService service = nuevoService();

        // El DAO debe respetar una transaccion que administra el llamador.
        int temporalId;
        try (Connection conexion = conexionSegura()) {
            conexion.setAutoCommit(false);
            try {
                EvaluacionProveedor temporal = jdbc.insertar(
                        conexion,
                        new EvaluacionProveedor(
                                d.proveedorA, d.loteId, 2, 2, FECHA_2,
                                "ROLLBACK MANUAL AUTOMATIZADO")
                );
                temporalId = temporal.getId();
                verificar(!conexion.isClosed(),
                        "DAO insertar deja abierta la conexion proporcionada");
                verificar(jdbc.buscarPorId(conexion, temporalId).isPresent(),
                        "La insercion es visible dentro de la misma transaccion");
            } finally {
                conexion.rollback();
            }
        }
        verificar(service.buscarPorId(temporalId).isEmpty(),
                "Rollback del llamador deshace el INSERT del DAO");

        // Doble de DAO: inserta de verdad y luego simula un fallo de negocio.
        AtomicInteger idAntesDelFallo = new AtomicInteger();
        EvaluacionProveedorDAO daoQueFalla = new EvaluacionProveedorDAO() {
            @Override
            public EvaluacionProveedor insertar(Connection c, EvaluacionProveedor e) {
                EvaluacionProveedor guardada = jdbc.insertar(c, e);
                idAntesDelFallo.set(guardada.getId());
                throw new IllegalStateException("Fallo simulado despues del INSERT");
            }

            @Override
            public java.util.Optional<EvaluacionProveedor> buscarPorId(Connection c, int id) {
                return jdbc.buscarPorId(c, id);
            }

            @Override
            public List<EvaluacionProveedor> listarPorProveedor(Connection c, int proveedorId) {
                return jdbc.listarPorProveedor(c, proveedorId);
            }

            @Override
            public boolean lotePerteneceAProveedor(Connection c, int loteId, int proveedorId) {
                return jdbc.lotePerteneceAProveedor(c, loteId, proveedorId);
            }
        };

        EvaluacionProveedorService serviceQueFalla = new EvaluacionProveedorService(
                daoQueFalla, EvaluacionProveedorTest::conexionSegura);
        esperar(IllegalStateException.class,
                () -> serviceQueFalla.registrar(new EvaluacionProveedor(
                        d.proveedorA, d.loteId, 3, 3, FECHA_2,
                        "ROLLBACK POR FALLO POST-INSERT")),
                "Service propaga la excepcion ocurrida despues del INSERT");
        verificar(idAntesDelFallo.get() > 0,
                "El doble efectivamente llego a insertar antes de fallar");
        verificar(service.buscarPorId(idAntesDelFallo.get()).isEmpty(),
                "Service ejecuto rollback real ante una excepcion posterior al INSERT");
        verificar(contarEvaluaciones(d.proveedorA) == 2,
                "Solo permanecen las dos evaluaciones confirmadas");
    }

    // ---------------------------------------------------------------------
    // 5. PREPARACION Y LIMPIEZA DE DATOS TEMPORALES
    // ---------------------------------------------------------------------

    private static DatosTemporales crearDatosTemporales() throws SQLException {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection conexion = conexionSegura()) {
            conexion.setAutoCommit(false);
            try {
                int proveedorA = insertarProveedor(conexion, "TEST-42-A-" + token);
                int proveedorB = insertarProveedor(conexion, "TEST-42-B-" + token);
                int loteId = insertarLote(conexion, "TEST-42-LOTE-" + token, proveedorA);
                conexion.commit();
                System.out.println("[DATOS] Proveedores temporales " + proveedorA
                        + ", " + proveedorB + "; lote " + loteId);
                return new DatosTemporales(proveedorA, proveedorB, loteId);
            } catch (SQLException | RuntimeException e) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            }
        }
    }

    private static int insertarProveedor(Connection c, String nombre) throws SQLException {
        String sql = "INSERT INTO proveedores (nombre, contacto, email) VALUES (?, ?, NULL)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, "Prueba automatizada #42");
            verificarFilas(ps.executeUpdate(), "No se pudo crear proveedor temporal");
            return obtenerClave(ps);
        }
    }

    private static int insertarLote(Connection c, String codigo, int proveedorId)
            throws SQLException {
        String sql = "INSERT INTO lotes (codigo, proveedor_id, fecha_ingreso) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, codigo);
            ps.setInt(2, proveedorId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2026, 11, 1)));
            verificarFilas(ps.executeUpdate(), "No se pudo crear lote temporal");
            return obtenerClave(ps);
        }
    }

    private static int obtenerClave(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (!rs.next()) {
                throw new SQLException("No se genero el ID de un dato temporal");
            }
            return rs.getInt(1);
        }
    }

    private static void verificarFilas(int filas, String mensaje) throws SQLException {
        if (filas != 1) {
            throw new SQLException(mensaje + ": filas = " + filas);
        }
    }

    private static void eliminarDatosTemporales(DatosTemporales d) throws SQLException {
        try (Connection conexion = conexionSegura()) {
            conexion.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conexion.prepareStatement(
                        "DELETE FROM evaluaciones_proveedor WHERE proveedor_id IN (?, ?)")) {
                    ps.setInt(1, d.proveedorA);
                    ps.setInt(2, d.proveedorB);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conexion.prepareStatement(
                        "DELETE FROM lotes WHERE id = ? AND proveedor_id = ?")) {
                    ps.setInt(1, d.loteId);
                    ps.setInt(2, d.proveedorA);
                    verificarFilas(ps.executeUpdate(), "No se elimino el lote de prueba");
                }
                try (PreparedStatement ps = conexion.prepareStatement(
                        "DELETE FROM proveedores WHERE id IN (?, ?)")) {
                    ps.setInt(1, d.proveedorA);
                    ps.setInt(2, d.proveedorB);
                    if (ps.executeUpdate() != 2) {
                        throw new SQLException("No se eliminaron los dos proveedores de prueba");
                    }
                }
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    conexion.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    private static long contarEvaluaciones(int proveedorId) throws SQLException {
        try (Connection conexion = conexionSegura();
             PreparedStatement ps = conexion.prepareStatement(
                     "SELECT COUNT(*) FROM evaluaciones_proveedor WHERE proveedor_id = ?")) {
            ps.setInt(1, proveedorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Cuenta y suma los importes historicos, sin modificar esos registros. */
    private static Map<String, Huella> leerHuellaHistorica() throws SQLException {
        Map<String, Huella> huellas = new LinkedHashMap<>();
        huellas.put("lotes_repuestos", huella("lotes_repuestos", "costo_unitario"));
        huellas.put("servicios_repuestos", huella("servicios_repuestos", "precio_unitario_venta"));
        huellas.put("facturas", huella("facturas", "total_final"));
        return huellas;
    }

    private static Huella huella(String tabla, String columna) throws SQLException {
        // Tabla/columna son constantes internas, nunca entrada del usuario.
        String sql = "SELECT COUNT(*), COALESCE(SUM(" + columna + "), 0) FROM " + tabla;
        try (Connection conexion = conexionSegura();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new Huella(rs.getLong(1), rs.getBigDecimal(2));
        }
    }

    private static EvaluacionProveedorService nuevoService() {
        return new EvaluacionProveedorService(
                new EvaluacionProveedorDAOFactory(),
                EvaluacionProveedorTest::conexionSegura
        );
    }

    private static EvaluacionProveedorController nuevoController() {
        return new EvaluacionProveedorController(nuevoService());
    }

    private static Connection conexionSegura() {
        Connection conexion = ConexionFactory.getInstancia().crearConexion();
        try {
            String catalogo = conexion.getCatalog();
            if (!BASE_PERMITIDA.equalsIgnoreCase(catalogo)) {
                try {
                    conexion.close();
                } catch (SQLException cierre) {
                    throw new IllegalStateException("Fallo al cerrar conexion insegura", cierre);
                }
                throw new IllegalStateException(
                        "TEST CANCELADO: ConexionFactory usa '" + catalogo
                                + "', pero solo se permite '" + BASE_PERMITIDA + "'");
            }
            return conexion;
        } catch (SQLException e) {
            try {
                conexion.close();
            } catch (SQLException cierre) {
                e.addSuppressed(cierre);
            }
            throw new IllegalStateException("No se pudo verificar la base de datos", e);
        }
    }

    private static void verificar(boolean condicion, String descripcion) {
        if (!condicion) {
            throw new AssertionError("[FALLO] " + descripcion);
        }
        verificaciones++;
        System.out.println("[OK] " + verificaciones + " - " + descripcion);
    }

    private static void esperar(Class<? extends Throwable> tipo,
                                Runnable accion,
                                String descripcion) {
        try {
            accion.run();
        } catch (Throwable error) {
            if (tipo.isInstance(error)) {
                verificar(true, descripcion);
                return;
            }
            throw new AssertionError("[FALLO] " + descripcion
                    + " | Excepcion inesperada: " + error, error);
        }
        throw new AssertionError("[FALLO] " + descripcion
                + " | Se esperaba " + tipo.getSimpleName());
    }

    private record DatosTemporales(int proveedorA, int proveedorB, int loteId) { }
    private record Huella(long filas, BigDecimal suma) { }
}
