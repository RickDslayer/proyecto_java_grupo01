package com.carmotors.servicio;

import com.carmotors.dao.ExistenciasDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.StockLote;
import com.carmotors.modelo.StockRepuesto;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/** Consultas de inventario; NO crea reservas, NO consume ni descuenta lotes. */
public class ExistenciasService {
    private final ExistenciasDAO dao;
    private final Supplier<Connection> conexiones;
    private final Supplier<LocalDate> hoy;

    public ExistenciasService(ExistenciasDAO dao, Supplier<Connection> conexiones, Supplier<LocalDate> hoy) {
        this.dao = java.util.Objects.requireNonNull(dao);
        this.conexiones = java.util.Objects.requireNonNull(conexiones);
        this.hoy = java.util.Objects.requireNonNull(hoy);
    }

    public List<StockLote> porRepuesto(int repuestoId) {
        validar(repuestoId, "repuesto");
        return consultar(con -> dao.porRepuesto(con, repuestoId));
    }

    /** Vacío si no existe la pareja (lote, repuesto). */
    public java.util.Optional<StockLote> porLote(int loteId, int repuestoId) {
        validar(loteId, "lote");
        validar(repuestoId, "repuesto");
        List<StockLote> filas = consultar(con -> dao.porLote(con, loteId, repuestoId));
        return filas.stream().findFirst();
    }

    /** Snapshot del repuesto: una consulta SQL y una fecha común para sumar los lotes. */
    public StockRepuesto resumen(int repuestoId) {
        validar(repuestoId, "repuesto");
        LocalDate fecha = hoy.get();
        List<StockLote> lotes = porRepuesto(repuestoId);
        long fisico = 0, reservado = 0, utilizable = 0;
        for (StockLote lote : lotes) {
            fisico = Math.addExact(fisico, lote.fisico());
            reservado = Math.addExact(reservado, lote.reservadoActivo());
            utilizable = Math.addExact(utilizable, lote.disponibleUtilizable(fecha));
        }
        return new StockRepuesto(repuestoId, fisico, reservado, fisico - reservado, utilizable);
    }

    private void validar(int id, String campo) {
        if (id <= 0) throw new ValidacionException("El id de " + campo + " debe ser mayor que cero.");
    }

    private <T> T consultar(java.util.function.Function<Connection,T> accion) {
        try (Connection con = conexiones.get()) {
            return accion.apply(con);
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión al consultar existencias.", e);
        }
    }
}
