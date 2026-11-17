package com.carmotors.dao;

import com.carmotors.modelo.RepuestoTipo;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Contrato de persistencia: usa la conexión del servicio, no hace commit ni cierra la conexión. */
public interface RepuestoTipoDAO {
    int crear(Connection con, String nombre);
    boolean actualizar(Connection con, int id, String nombre);
    boolean eliminar(Connection con, int id);
    List<RepuestoTipo> listar(Connection con);
    Optional<RepuestoTipo> buscarPorId(Connection con, int id);
    List<RepuestoTipo> buscarPorNombre(Connection con, String nombre);
}
