package com.carmotors.servicio;

import com.carmotors.dao.ServicioTecnicoDAO;
import com.carmotors.dao.factory.DAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.ServicioTecnico;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** Asignaciones N:M; cada escritura bloquea primero la orden y luego el técnico. */
public class AsignacionTecnicoService {
    private final ServicioTecnicoDAO dao;
    private final Supplier<Connection> conexiones;

    public AsignacionTecnicoService(DAOFactory<ServicioTecnicoDAO> creador, Supplier<Connection> conexiones) {
        this(creador.obtenerDAO(), conexiones);
    }
    public AsignacionTecnicoService(ServicioTecnicoDAO dao, Supplier<Connection> conexiones) {
        this.dao = Objects.requireNonNull(dao);
        this.conexiones = Objects.requireNonNull(conexiones);
    }

    public void asignar(int ordenId, int tecnicoId) {
        ids(ordenId, tecnicoId);
        transaccion(con -> {
            boolean activo = validarReferencias(con, ordenId, tecnicoId);
            if (!activo) throw new ValidacionException("El técnico está inactivo y no puede recibir asignaciones nuevas.");
            if (dao.existe(con, ordenId, tecnicoId)) throw new ValidacionException("El técnico ya está asignado a esta orden.");
            dao.asignar(con, ordenId, tecnicoId);
            return null;
        });
    }

    public void quitar(int ordenId, int tecnicoId) {
        ids(ordenId, tecnicoId);
        transaccion(con -> {
            validarReferencias(con, ordenId, tecnicoId);
            if (!dao.quitar(con, ordenId, tecnicoId)) throw new ValidacionException("El técnico no está asignado a esta orden.");
            return null;
        });
    }

    /** Sustituye el total de horas, no acumula ni duplica al repetir la petición. */
    public void registrarHoras(int ordenId, int tecnicoId, BigDecimal horas) {
        ids(ordenId, tecnicoId);
        if (horas == null || horas.signum() < 0 || horas.compareTo(new BigDecimal("999.99")) > 0) {
            throw new ValidacionException("Las horas deben estar entre 0 y 999.99.");
        }
        BigDecimal total;
        try { total = horas.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new ValidacionException("Las horas admiten como máximo dos decimales."); }
        transaccion(con -> {
            validarReferencias(con, ordenId, tecnicoId);
            if (!dao.existe(con, ordenId, tecnicoId)) throw new ValidacionException("El técnico no está asignado a esta orden.");
            // MySQL puede informar cero filas modificadas si el valor ya era el mismo.
            dao.actualizarHoras(con, ordenId, tecnicoId, total);
            return null;
        });
    }

    public List<ServicioTecnico> listarPorOrden(int id) {
        id(id);
        return consultar(con -> dao.listarPorOrden(con, id));
    }
    public List<ServicioTecnico> listarPorTecnico(int id) {
        id(id);
        return consultar(con -> dao.listarPorTecnico(con, id));
    }

    private boolean validarReferencias(Connection con, int ordenId, int tecnicoId) {
        String estado = dao.bloquearEstadoOrden(con, ordenId)
                .orElseThrow(() -> new ValidacionException("No existe la orden indicada."));
        if (!List.of("Pendiente", "En Proceso").contains(estado)) {
            throw new ValidacionException("La orden está cerrada o su estado no permite modificar asignaciones.");
        }
        return dao.bloquearTecnicoActivo(con, tecnicoId)
                .orElseThrow(() -> new ValidacionException("No existe el técnico indicado."));
    }

    private void ids(int orden, int tecnico) { id(orden); id(tecnico); }
    private void id(int id) {
        if (id <= 0) throw new ValidacionException("Los identificadores deben ser mayores que cero.");
    }

    private <T> T consultar(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) { return accion.apply(con); }
        catch (SQLException e) { throw new DaoException("No se pudo cerrar la conexión de asignaciones.", e); }
    }

    private <T> T transaccion(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            con.setAutoCommit(false);
            try {
                T resultado = accion.apply(con);
                con.commit();
                return resultado;
            } catch (RuntimeException | SQLException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                for (Throwable causa = e; causa != null; causa = causa.getCause()) {
                    if (causa instanceof SQLException sql && sql.getErrorCode() == 1062) {
                        throw new ValidacionException("El técnico ya está asignado a esta orden.");
                    }
                }
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new DaoException("No se pudo confirmar la asignación.", e);
            }
        } catch (SQLException e) { throw new DaoException("No se pudo administrar la conexión de asignaciones.", e); }
    }
}
