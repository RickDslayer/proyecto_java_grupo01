package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.OrdenServicio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrdenServicioDAOJdbc implements OrdenServicioDAO {
    private static final String CONSULTA = "SELECT s.id, s.vehiculo_id, v.cliente_id, v.placa, "
            + "mt.nombre AS tipo, e.nombre AS estado, s.fecha_recepcion, s.fecha_completado, "
            + "s.fecha_entrega, s.descripcion_problema, "
            + "COALESCE((SELECT SUM(d.precio_cobrado) FROM servicios_detalle d WHERE d.servicio_id = s.id), 0) AS mano_obra "
            + "FROM servicios s JOIN vehiculos v ON v.id = s.vehiculo_id "
            + "JOIN mantenimiento_tipos mt ON mt.id = s.mantenimiento_tipo_id "
            + "JOIN servicio_estados e ON e.id = s.estado_id ";

    @Override
    public int crear(Connection con, OrdenServicio orden) {
        String sql = "INSERT INTO servicios (vehiculo_id, mantenimiento_tipo_id, estado_id, fecha_recepcion, descripcion_problema) "
                + "SELECT ?, mt.id, e.id, ?, ? FROM mantenimiento_tipos mt CROSS JOIN servicio_estados e "
                + "WHERE mt.nombre = ? AND e.nombre = ?";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orden.getVehiculoId());
            ps.setTimestamp(2, Timestamp.valueOf(orden.getFechaRecepcion()));
            ps.setString(3, orden.getDescripcionProblema());
            ps.setString(4, orden.getTipoMantenimiento());
            ps.setString(5, orden.getEstado());
            if (ps.executeUpdate() != 1) {
                throw new DaoException("Faltan catálogos de mantenimiento/estado. Ejecuta database/actualizar-estados-ordenes.sql.");
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el id de la orden.");
                return claves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo registrar la orden de servicio.", e);
        }
    }

    @Override public Optional<OrdenServicio> buscarPorId(Connection con, int id) {
        return consultar(con, "WHERE s.id = ?", id).stream().findFirst();
    }
    @Override public List<OrdenServicio> listar(Connection con) {
        return consultar(con, "", null);
    }
    @Override public List<OrdenServicio> listarPorVehiculo(Connection con, int id) {
        return consultar(con, "WHERE s.vehiculo_id = ?", id);
    }
    @Override public List<OrdenServicio> listarPorEstado(Connection con, String estado) {
        return consultar(con, "WHERE e.nombre = ?", estado);
    }

    private List<OrdenServicio> consultar(Connection con, String filtro, Object valor) {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + filtro + " ORDER BY s.fecha_recepcion, s.id")) {
            if (valor instanceof Integer id) ps.setInt(1, id);
            else if (valor instanceof String texto) ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrdenServicio> ordenes = new ArrayList<>();
                while (rs.next()) {
                    OrdenServicio o = new OrdenServicio();
                    o.setId(rs.getInt("id")); o.setVehiculoId(rs.getInt("vehiculo_id"));
                    o.setClienteId(rs.getInt("cliente_id")); o.setPlaca(rs.getString("placa"));
                    o.setTipoMantenimiento(rs.getString("tipo")); o.setEstado(rs.getString("estado"));
                    o.setFechaRecepcion(rs.getTimestamp("fecha_recepcion").toLocalDateTime());
                    Timestamp completado = rs.getTimestamp("fecha_completado");
                    Timestamp entrega = rs.getTimestamp("fecha_entrega");
                    o.setFechaCompletado(completado == null ? null : completado.toLocalDateTime());
                    o.setFechaEntrega(entrega == null ? null : entrega.toLocalDateTime());
                    o.setDescripcionProblema(rs.getString("descripcion_problema"));
                    o.setCostoManoObra(rs.getBigDecimal("mano_obra"));
                    ordenes.add(o);
                }
                return ordenes;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar las órdenes.", e);
        }
    }
}
