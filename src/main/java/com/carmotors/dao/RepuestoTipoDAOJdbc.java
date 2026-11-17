package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.RepuestoTipo;
import java.sql.*;
import java.util.*;

/** Único lugar con SQL para el catálogo repuesto_tipos. */
public final class RepuestoTipoDAOJdbc implements RepuestoTipoDAO {
    @Override
    public int crear(Connection con, String nombre) {
        String sql = "INSERT INTO repuesto_tipos (nombre) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new DaoException("MySQL no devolvió el id del tipo creado.");
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw traducir(e, "No se pudo crear el tipo de repuesto.");
        }
    }

    @Override
    public boolean actualizar(Connection con, int id, String nombre) {
        String sql = "UPDATE repuesto_tipos SET nombre = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducir(e, "No se pudo actualizar el tipo de repuesto.");
        }
    }

    @Override
    public boolean eliminar(Connection con, int id) {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM repuesto_tipos WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducir(e, "No se pudo eliminar el tipo de repuesto.");
        }
    }

    @Override
    public List<RepuestoTipo> listar(Connection con) {
        return consultar(con, "SELECT id, nombre FROM repuesto_tipos ORDER BY id", null);
    }

    @Override
    public Optional<RepuestoTipo> buscarPorId(Connection con, int id) {
        List<RepuestoTipo> filas = consultar(con,
                "SELECT id, nombre FROM repuesto_tipos WHERE id = ?", id);
        return filas.stream().findFirst();
    }

    @Override
    public List<RepuestoTipo> buscarPorNombre(Connection con, String nombre) {
        return consultar(con,
                "SELECT id, nombre FROM repuesto_tipos WHERE nombre LIKE ? ORDER BY nombre", "%" + nombre + "%");
    }

    private List<RepuestoTipo> consultar(Connection con, String sql, Object parametro) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                List<RepuestoTipo> resultado = new ArrayList<>();
                while (rs.next()) resultado.add(new RepuestoTipo(rs.getInt("id"), rs.getString("nombre")));
                return resultado;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar los tipos de repuesto.", e);
        }
    }

    private RuntimeException traducir(SQLException e, String mensaje) {
        return switch (e.getErrorCode()) {
            case 1062 -> new ValidacionException("Ya existe un tipo con ese nombre.");
            case 1451 -> new ValidacionException("No se puede eliminar: hay repuestos asociados a este tipo.");
            case 1406 -> new ValidacionException("El nombre del tipo excede los 100 caracteres.");
            default -> new DaoException(mensaje, e);
        };
    }
}
