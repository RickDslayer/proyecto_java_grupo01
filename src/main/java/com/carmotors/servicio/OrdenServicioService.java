package com.carmotors.servicio;

import com.carmotors.dao.OrdenServicioDAO;
import com.carmotors.dao.factory.DAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.modelo.Vehiculo;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Tarea 05: alta y consulta, sin asignaciones, reservas ni transiciones posteriores. */
public class OrdenServicioService {
    private final OrdenServicioDAO dao;
    private final VehiculoConsulta vehiculos;
    private final Supplier<Connection> conexiones;
    private final Clock reloj;

    public OrdenServicioService(DAOFactory<OrdenServicioDAO> creador, VehiculoConsulta vehiculos,
                                Supplier<Connection> conexiones, Clock reloj) {
        this(creador.obtenerDAO(), vehiculos, conexiones, reloj);
    }

    public OrdenServicioService(OrdenServicioDAO dao, VehiculoConsulta vehiculos,
                                Supplier<Connection> conexiones, Clock reloj) {
        this.dao = Objects.requireNonNull(dao);
        this.vehiculos = Objects.requireNonNull(vehiculos);
        this.conexiones = Objects.requireNonNull(conexiones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public OrdenServicio crearOrden(int vehiculoId, String mantenimiento, String problema) {
        validarId(vehiculoId);
        String tipo = opcion(mantenimiento, List.of("Preventivo", "Correctivo"), "tipo de mantenimiento");
        if (problema == null || problema.isBlank()) {
            throw new ValidacionException("La descripción del problema es obligatoria.");
        }
        String descripcion = problema.strip();
        if (descripcion.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new ValidacionException("La descripción supera la capacidad del campo TEXT (65535 bytes UTF-8).");
        }
        Vehiculo vehiculo = vehiculos.buscarPorId(vehiculoId)
                .orElseThrow(() -> new ValidacionException("No existe el vehículo indicado."));
        OrdenServicio orden = new OrdenServicio();
        orden.setVehiculoId(vehiculoId);
        orden.setClienteId(vehiculo.getClienteId());
        orden.setPlaca(vehiculo.getPlaca());
        orden.setTipoMantenimiento(tipo);
        orden.setEstado("Pendiente");
        orden.setFechaRecepcion(LocalDateTime.now(reloj).withNano(0));
        orden.setDescripcionProblema(descripcion);
        // Importe derivado de detalles: una orden nueva no tiene operaciones cobradas.
        orden.setCostoManoObra(new BigDecimal("0.00"));
        try (Connection con = conexiones.get()) {
            con.setAutoCommit(false);
            try {
                int id = dao.crear(con, orden);
                con.commit();
                orden.setId(id);
                return orden;
            } catch (RuntimeException | SQLException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new DaoException("No se pudo confirmar el registro de la orden.", e);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo administrar la conexión de órdenes.", e);
        }
    }

    public Optional<OrdenServicio> buscarPorId(int id) {
        validarId(id);
        return consultar(con -> dao.buscarPorId(con, id));
    }

    public List<OrdenServicio> listar() {
        return consultar(dao::listar);
    }

    public List<OrdenServicio> listarPorVehiculo(int vehiculoId) {
        validarId(vehiculoId);
        return consultar(con -> dao.listarPorVehiculo(con, vehiculoId));
    }

    public List<OrdenServicio> listarPorEstado(String estado) {
        String valido = opcion(estado, List.of("Pendiente", "En Proceso", "Completado", "Facturado", "Entregado"), "estado");
        return consultar(con -> dao.listarPorEstado(con, valido));
    }

    private String opcion(String valor, List<String> permitidos, String campo) {
        if (valor == null) throw new ValidacionException("Debes indicar el " + campo + ".");
        return permitidos.stream().filter(p -> p.equalsIgnoreCase(valor.strip())).findFirst()
                .orElseThrow(() -> new ValidacionException("El " + campo + " debe ser: " + String.join(", ", permitidos) + "."));
    }

    private void validarId(int id) {
        if (id <= 0) throw new ValidacionException("El identificador debe ser mayor que cero.");
    }

    private <T> T consultar(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            return accion.apply(con);
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión al consultar órdenes.", e);
        }
    }
}
