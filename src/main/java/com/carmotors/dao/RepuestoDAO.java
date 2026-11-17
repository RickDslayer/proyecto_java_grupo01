package com.carmotors.dao;

import com.carmotors.modelo.Repuesto;
import java.sql.Connection;
import java.util.List;

/** Acceso a datos de repuestos. Recibe la conexión: no la cierra ni hace commit. */
public interface RepuestoDAO {
    int crear(Connection con, Repuesto repuesto);
    boolean actualizar(Connection con, Repuesto repuesto);
    boolean eliminar(Connection con, int id);

    List<Repuesto> listar(Connection con);
    List<Repuesto> buscarPorId(Connection con, int id);
    List<Repuesto> buscarPorNombre(Connection con, String texto);
    List<Repuesto> listarPorTipo(Connection con, int tipoId);
    List<Repuesto> listarPorMarca(Connection con, int marcaId);
    List<Repuesto> listarPorProveedor(Connection con, int proveedorId);
    List<Repuesto> listarStockBajo(Connection con);
}