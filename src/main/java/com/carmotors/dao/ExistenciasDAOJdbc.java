package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.StockLote;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL concentrado en DAO. Reserva ACTIVA solamente, sin restar CONSUMIDA ni LIBERADA. */
public class ExistenciasDAOJdbc implements ExistenciasDAO {
    private static final String BASE = "SELECT lr.lote_id, l.codigo, lr.repuesto_id, lr.fecha_caducidad, "
            + "lr.cantidad_actual AS fisico, COALESCE(ra.total, 0) AS reservado "
            + "FROM lotes_repuestos lr JOIN lotes l ON l.id = lr.lote_id "
            + "LEFT JOIN (SELECT rr.lote_id, rr.repuesto_id, SUM(rr.cantidad) AS total "
            + "FROM reservas_repuestos rr JOIN reserva_estados re ON re.id = rr.estado_id "
            + "WHERE re.nombre = 'ACTIVA' GROUP BY rr.lote_id, rr.repuesto_id) ra "
            + "ON ra.lote_id = lr.lote_id AND ra.repuesto_id = lr.repuesto_id ";

    @Override
    public List<StockLote> porRepuesto(Connection conexion, int repuestoId) {
        return consultar(conexion, BASE + "WHERE lr.repuesto_id = ? ORDER BY lr.lote_id", repuestoId, null);
    }

    @Override
    public List<StockLote> porLote(Connection conexion, int loteId, int repuestoId) {
        return consultar(conexion, BASE + "WHERE lr.lote_id = ? AND lr.repuesto_id = ?", loteId, repuestoId);
    }

    private List<StockLote> consultar(Connection conexion, String sql, int primero, Integer segundo) {
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, primero);
            if (segundo != null) ps.setInt(2, segundo);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockLote> resultado = new ArrayList<>();
                while (rs.next()) {
                    java.sql.Date caducidad = rs.getDate("fecha_caducidad");
                    resultado.add(new StockLote(rs.getInt("lote_id"), rs.getString("codigo"),
                            rs.getInt("repuesto_id"), caducidad == null ? null : caducidad.toLocalDate(),
                            rs.getLong("fisico"), rs.getLong("reservado")));
                }
                return resultado;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar las existencias.", e);
        }
    }
}
