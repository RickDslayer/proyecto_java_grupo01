package com.carmotors;

import com.carmotors.config.ConexionFactory;
import com.carmotors.controlador.EvaluacionProveedorController;
import com.carmotors.dao.factory.EvaluacionProveedorDAOFactory;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.EvaluacionProveedor;
import com.carmotors.servicio.EvaluacionProveedorService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Scanner;
import java.util.function.Supplier;

/**
 * Vista MANUAL de pruebas para el issue #42, con Scanner y main().
 *
 * Solo trabaja con carmotors_test; no hay sentencias SQL en la vista.
 * Los datos que registres quedan guardados tras el commit: no se limpian
 * automaticamente, a diferencia de EvaluacionProveedorTest.
 */
public final class VistaPruebaEvaluacionProveedor {

    private static final String BASE_PERMITIDA = "carmotors_test";

    private VistaPruebaEvaluacionProveedor() { }

    public static void main(String[] args) {
        Supplier<Connection> conexiones = VistaPruebaEvaluacionProveedor::conexionSegura;

        // Comprobacion previa: evita abrir el menu si la base es incorrecta.
        try (Connection ignorada = conexiones.get()) {
            System.out.println("Conexion validada: " + BASE_PERMITIDA);
        } catch (SQLException | RuntimeException e) {
            System.err.println("No se puede iniciar la prueba: " + e.getMessage());
            return;
        }

        // Aqui se utiliza Factory Method para obtener el DAOJdbc.
        EvaluacionProveedorService servicio = new EvaluacionProveedorService(
                new EvaluacionProveedorDAOFactory(), conexiones);
        EvaluacionProveedorController controller =
                new EvaluacionProveedorController(servicio);

        try (Scanner scanner = new Scanner(System.in)) {
            ejecutarMenu(scanner, controller);
        }
    }

    private static void ejecutarMenu(
            Scanner scanner, EvaluacionProveedorController controller) {
        String opcion;

        do {
            imprimirMenu();
            opcion = solicitar(scanner, "Seleccione una opcion: ");

            try {
                switch (opcion) {
                    case "1" -> registrar(scanner, controller);
                    case "2" -> buscar(scanner, controller);
                    case "3" -> listar(scanner, controller);
                    case "4" -> mostrarPromedios(scanner, controller);
                    case "5" -> mostrarAyuda();
                    case "0" -> System.out.println("Fin de la prueba manual.");
                    default -> System.out.println("Opcion no reconocida.");
                }
            } catch (ValidacionException | IllegalArgumentException e) {
                System.out.println("[VALIDACION] " + e.getMessage());
            } catch (RuntimeException e) {
                System.err.println("[ERROR] " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("[CAUSA] " + e.getCause().getMessage());
                }
                // No se oculta una falla inesperada durante las pruebas.
                e.printStackTrace(System.err);
            }
        } while (!"0".equals(opcion));
    }

    private static void imprimirMenu() {
        System.out.println("\n==============================================");
        System.out.println("   CARMOTORS - EVALUACION DE PROVEEDORES");
        System.out.println("   Issue #42 | Pruebas manuales - MySQL");
        System.out.println("==============================================");
        System.out.println("1. Registrar una evaluacion");
        System.out.println("2. Buscar evaluacion por ID");
        System.out.println("3. Listar evaluaciones de un proveedor");
        System.out.println("4. Consultar promedios de un proveedor");
        System.out.println("5. Ayuda y escenarios de prueba");
        System.out.println("0. Salir");
    }

    private static void registrar(
            Scanner scanner, EvaluacionProveedorController controller) {
        System.out.println("\n--- REGISTRAR EVALUACION ---");
        int proveedorId = solicitarEntero(scanner, "ID del proveedor: ");
        int loteId = solicitarEntero(scanner, "ID del lote: ");
        String calidad = solicitar(scanner, "Calidad [1-5]: ");
        String puntualidad = solicitar(scanner, "Puntualidad [1-5]: ");
        String fecha = solicitar(scanner, "Fecha [yyyy-MM-dd] (vacio = hoy): ");
        if (fecha.isBlank()) {
            fecha = LocalDate.now().toString();
        }
        String observaciones = solicitar(scanner, "Observaciones (opcional): ");

        // La vista no construye SQL ni administra transacciones.
        EvaluacionProveedor guardada = controller.registrarEvaluacion(
                proveedorId, loteId, calidad, puntualidad, fecha, observaciones);
        System.out.println("[GUARDADA] MySQL asigno el ID " + guardada.getId());
        mostrarEvaluacion(guardada);
    }

    private static void buscar(
            Scanner scanner, EvaluacionProveedorController controller) {
        System.out.println("\n--- BUSCAR POR ID ---");
        int id = solicitarEntero(scanner, "ID de la evaluacion: ");
        Optional<EvaluacionProveedor> resultado = controller.buscarEvaluacionPorId(id);
        if (resultado.isPresent()) {
            mostrarEvaluacion(resultado.get());
        } else {
            System.out.println("No existe una evaluacion con ese ID.");
        }
    }

    private static void listar(
            Scanner scanner, EvaluacionProveedorController controller) {
        System.out.println("\n--- LISTAR POR PROVEEDOR ---");
        int id = solicitarEntero(scanner, "ID del proveedor: ");
        List<EvaluacionProveedor> evaluaciones = controller.listarPorProveedor(id);
        if (evaluaciones.isEmpty()) {
            System.out.println("No hay evaluaciones registradas para este proveedor.");
            return;
        }
        System.out.println("Total de evaluaciones: " + evaluaciones.size());
        for (EvaluacionProveedor evaluacion : evaluaciones) {
            mostrarEvaluacion(evaluacion);
        }
    }

    private static void mostrarPromedios(
            Scanner scanner, EvaluacionProveedorController controller) {
        System.out.println("\n--- PROMEDIOS POR PROVEEDOR ---");
        int id = solicitarEntero(scanner, "ID del proveedor: ");
        OptionalDouble calidad = controller.promedioCalidad(id);
        OptionalDouble puntualidad = controller.promedioPuntualidad(id);

        if (calidad.isEmpty() && puntualidad.isEmpty()) {
            System.out.println("Sin evaluaciones: no hay puntuacion disponible.");
            return;
        }
        System.out.printf(java.util.Locale.ROOT,
                "Promedio de calidad: %.2f / 5%n", calidad.orElseThrow());
        System.out.printf(java.util.Locale.ROOT,
                "Promedio de puntualidad: %.2f / 5%n", puntualidad.orElseThrow());
    }

    private static void mostrarEvaluacion(EvaluacionProveedor evaluacion) {
        System.out.println("----------------------------------------------");
        System.out.println("ID:          " + evaluacion.getId());
        System.out.println("Proveedor:   " + evaluacion.getProveedorId());
        System.out.println("Lote:        " + evaluacion.getLoteId());
        System.out.println("Calidad:     " + evaluacion.getCalificacionCalidad() + "/5");
        System.out.println("Puntualidad: " + evaluacion.getCalificacionPuntualidad() + "/5");
        System.out.println("Fecha:       " + evaluacion.getFechaEvaluacion());
        System.out.println("Observacion: " + (evaluacion.getObservaciones() == null
                ? "Sin observaciones" : evaluacion.getObservaciones()));
    }

    private static void mostrarAyuda() {
        System.out.println("\n--- ESCENARIOS DE PRUEBA ---");
        System.out.println("Si cargaste database/data.sql, prueba:");
        System.out.println("- Proveedor 2 / lote 1: relacion valida.");
        System.out.println("- Proveedor 1 / lote 2: relacion valida.");
        System.out.println("- Proveedor 3 / lote 3: relacion valida.");
        System.out.println("- Proveedor 4 / lote 5: relacion valida.");
        System.out.println("- Proveedor 1 / lote 1: DEBE RECHAZARSE (otro proveedor).");
        System.out.println("- Calidad 0 o 6: DEBE RECHAZARSE.");
        System.out.println("- Puntualidad 'abc': DEBE RECHAZARSE.");
        System.out.println("- Proveedor sin evaluaciones: mostrar 'sin puntuacion'.");
        System.out.println("IMPORTANTE: las evaluaciones registradas NO se borran al salir.");
        System.out.println("Usa IDs reales de tu base: los datos iniciales pueden cambiar.");
    }

    private static int solicitarEntero(Scanner scanner, String pregunta) {
        String texto = solicitar(scanner, pregunta);
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new ValidacionException("Introduce un ID entero valido.");
        }
    }

    private static String solicitar(Scanner scanner, String pregunta) {
        System.out.print(pregunta);
        return scanner.nextLine().strip();
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
                        "Operacion cancelada: se detecto '" + catalogo
                                + "'. Solo se permite '" + BASE_PERMITIDA + "'.");
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
}
