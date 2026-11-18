package com.carmotors.servicio;

import com.carmotors.dao.RepuestoTipoDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.RepuestoTipo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Validaciones y transacciones del CRUD de tipos de repuesto. */
public final class RepuestoTipoService {
    private final RepuestoTipoDAO dao;
    private final Supplier<Connection> conexiones;

    public RepuestoTipoService(RepuestoTipoDAO dao, Supplier<Connection> conexiones) {
        this.dao = Objects.requireNonNull(dao);
        this.conexiones = Objects.requireNonNull(conexiones);
    }

    public int crear(String nombre) {
        String valido = validarNombre(nombre);
        return escribir(con -> dao.crear(con, valido));
    }

    public boolean actualizar(int id, String nombre) {
        validarId(id);
        String valido = validarNombre(nombre);
        return escribir(con -> dao.actualizar(con, id, valido));
    }

    public boolean eliminar(int id) {
        validarId(id);
        return escribir(con -> dao.eliminar(con, id));
    }

    public List<RepuestoTipo> listar() {
        return leer(dao::listar);
    }

    public Optional<RepuestoTipo> buscarPorId(int id) {
        validarId(id);
        return leer(con -> dao.buscarPorId(con, id));
    }

    public List<RepuestoTipo> buscarPorNombre(String nombre) {
        String buscado = nombre == null ? "" : nombre.trim();
        return leer(con -> dao.buscarPorNombre(con, buscado));
    }

    private void validarId(int id) {
        if (id <= 0) throw new ValidacionException("El ID debe ser mayor que cero.");
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank())
            throw new ValidacionException("El nombre del tipo es obligatorio.");
        String normalizado = nombre.trim();
        if (normalizado.length() > 100)
            throw new ValidacionException("El nombre del tipo no puede superar 100 caracteres.");
        return normalizado;
    }

    private <T> T leer(Function<Connection, T> operacion) {
        try (Connection con = conexiones.get()) {
            return operacion.apply(con);
        } catch (SQLException e) {
            throw new DaoException("Error al cerrar la conexión de lectura de tipos.", e);
        }
    }

    private <T> T escribir(Function<Connection, T> operacion) {
        try (Connection con = conexiones.get()) {
            con.setAutoCommit(false);
            try {
                T resultado = operacion.apply(con);
                con.commit();
                return resultado;
            } catch (RuntimeException | SQLException e) {
                try { con.rollback(); } catch (SQLException falloRollback) { e.addSuppressed(falloRollback); }
                if (e instanceof RuntimeException re) throw re;
                throw new DaoException("No se pudo confirmar la transacción de tipos.", e);
            }
        } catch (SQLException e) {
            throw new DaoException("Error al abrir o cerrar la transacción de tipos.", e);
        }
    }
}
