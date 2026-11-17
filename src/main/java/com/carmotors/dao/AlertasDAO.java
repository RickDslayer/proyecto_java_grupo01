package com.carmotors.dao;

import com.carmotors.modelo.RepuestoMinimo;
import java.sql.Connection;
import java.util.List;

public interface AlertasDAO {
    List<RepuestoMinimo> listarRepuestos(Connection conexion);
}
