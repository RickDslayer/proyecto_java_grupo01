package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.RepuestoMinimo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Único lugar con SQL para obtener el mínimo configurado por repuesto. */
public class AlertasDAOJdbc implements AlertasDAO {
    @Override
    public List<RepuestoMinimo> listarRepuestos(Connection conexion) {
        String sql = "SELECT id, nombre, stock_minimo FROM repuestos ORDER BY id";
        List<RepuestoMinimo> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new RepuestoMinimo(rs.getInt("id"), rs.getString("nombre"),
                        rs.getInt("stock_minimo")));
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar los mínimos de inventario.", e);
        }
    }
}
