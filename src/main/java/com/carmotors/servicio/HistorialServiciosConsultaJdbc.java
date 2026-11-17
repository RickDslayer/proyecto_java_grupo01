
package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.HistorialServiciosDAOJdbc;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.ServicioHistorial;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class HistorialServiciosConsultaJdbc
        implements HistorialServiciosConsulta {

    private final HistorialServiciosDAOJdbc historialDAO;

    public HistorialServiciosConsultaJdbc(
            HistorialServiciosDAOJdbc historialDAO) {

        this.historialDAO = Objects.requireNonNull(
                historialDAO,
                "El DAO de historial es obligatorio"
        );
    }

    @Override
    public List<ServicioHistorial> listarPorVehiculo(
            int vehiculoId) {

        if (vehiculoId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del vehiculo debe ser positivo"
            );
        }

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            return historialDAO.listarPorVehiculo(
                    conexion,
                    vehiculoId
            );

        } catch (SQLException e) {

            throw new AccesoDatosException(
                    "Error al cerrar la conexion del historial", e
            );
        }
    }
}
