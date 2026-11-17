package com.carmotors.dao;

import com.carmotors.modelo.OrdenServicio;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Alta y consultas; la Connection y la transacción pertenecen al servicio. */
public interface OrdenServicioDAO {
    int crear(Connection con, OrdenServicio orden);
    Optional<OrdenServicio> buscarPorId(Connection con, int id);
    List<OrdenServicio> listar(Connection con);
    List<OrdenServicio> listarPorVehiculo(Connection con, int vehiculoId);
    List<OrdenServicio> listarPorEstado(Connection con, String estado);
}
