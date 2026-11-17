package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.ServicioTecnico;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServicioTecnicoDAOJdbc implements ServicioTecnicoDAO {
    @Override
    public Optional<String> bloquearEstadoOrden(Connection con, int servicioId) {
        String sql = "SELECT e.nombre FROM servicios s JOIN servicio_estados e ON e.id = s.estado_id WHERE s.id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, servicioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DaoException("No se pudo bloquear la orden para asignación.", e); }
    }

    @Override
    public Optional<Boolean> bloquearTecnicoActivo(Connection con, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("SELECT activo FROM tecnicos WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, tecnicoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBoolean(1)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DaoException("No se pudo bloquear el técnico para asignación.", e); }
    }

    @Override
    public boolean existe(Connection con, int servicioId, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM servicios_tecnicos WHERE servicio_id = ? AND tecnico_id = ?")) {
            ids(ps, servicioId, tecnicoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DaoException("No se pudo consultar la asignación.", e); }
    }

    @Override
    public void asignar(Connection con, int servicioId, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO servicios_tecnicos (servicio_id, tecnico_id, horas_trabajadas) VALUES (?, ?, 0.00)")) {
            ids(ps, servicioId, tecnicoId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DaoException("No se pudo asignar el técnico.", e); }
    }

    @Override
    public boolean quitar(Connection con, int servicioId, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM servicios_tecnicos WHERE servicio_id = ? AND tecnico_id = ?")) {
            ids(ps, servicioId, tecnicoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DaoException("No se pudo quitar la asignación.", e); }
    }

    @Override
    public boolean actualizarHoras(Connection con, int servicioId, int tecnicoId, BigDecimal horas) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE servicios_tecnicos SET horas_trabajadas = ? WHERE servicio_id = ? AND tecnico_id = ?")) {
            ps.setBigDecimal(1, horas); ps.setInt(2, servicioId); ps.setInt(3, tecnicoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DaoException("No se pudieron actualizar las horas del técnico.", e); }
    }

    @Override public List<ServicioTecnico> listarPorOrden(Connection con, int id) {
        return listar(con, "st.servicio_id", id);
    }
    @Override public List<ServicioTecnico> listarPorTecnico(Connection con, int id) {
        return listar(con, "st.tecnico_id", id);
    }

    private List<ServicioTecnico> listar(Connection con, String campo, int id) {
        String sql = "SELECT st.servicio_id, st.tecnico_id, t.nombre, e.nombre AS estado, st.horas_trabajadas "
                + "FROM servicios_tecnicos st JOIN tecnicos t ON t.id = st.tecnico_id "
                + "JOIN servicios s ON s.id = st.servicio_id JOIN servicio_estados e ON e.id = s.estado_id "
                + "WHERE " + campo + " = ? ORDER BY st.servicio_id, st.tecnico_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<ServicioTecnico> filas = new ArrayList<>();
                while (rs.next()) filas.add(new ServicioTecnico(rs.getInt("servicio_id"), rs.getInt("tecnico_id"),
                        rs.getString("nombre"), rs.getString("estado"), rs.getBigDecimal("horas_trabajadas")));
                return filas;
            }
        } catch (SQLException e) { throw new DaoException("No se pudieron consultar las asignaciones.", e); }
    }

    private void ids(PreparedStatement ps, int servicioId, int tecnicoId) throws SQLException {
        ps.setInt(1, servicioId); ps.setInt(2, tecnicoId);
    }
}
