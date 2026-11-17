package com.carmotors.servicio;

import com.carmotors.dao.AlertasDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.AlertaInventario;
import com.carmotors.modelo.RepuestoMinimo;
import com.carmotors.modelo.StockLote;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Alertas calculadas desde existencias actuales, sin persistencia de notificaciones. */
public class AlertasInventarioService implements ObservadorInventario {
    private final AlertasDAO dao;
    private final ExistenciasService existencias;
    private final Supplier<Connection> conexiones;
    private final Supplier<LocalDate> hoy;
    private final int diasAviso;
    private volatile List<AlertaInventario> ultimas = List.of();

    public AlertasInventarioService(AlertasDAO dao, ExistenciasService existencias,
                                   Supplier<Connection> conexiones, Supplier<LocalDate> hoy,
                                   int diasAviso) {
        if (diasAviso < 0) throw new ValidacionException("Los días de aviso no pueden ser negativos.");
        this.dao = Objects.requireNonNull(dao);
        this.existencias = Objects.requireNonNull(existencias);
        this.conexiones = Objects.requireNonNull(conexiones);
        this.hoy = Objects.requireNonNull(hoy);
        this.diasAviso = diasAviso;
    }

    public List<AlertaInventario> consultarAlertas() {
        LocalDate fecha = hoy.get();
        LocalDate limite = fecha.plusDays(diasAviso);
        List<RepuestoMinimo> repuestos;
        try (Connection con = conexiones.get()) {
            repuestos = dao.listarRepuestos(con);
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión al consultar alertas.", e);
        }
        List<AlertaInventario> alertas = new ArrayList<>();
        for (RepuestoMinimo repuesto : repuestos) {
            List<StockLote> lotes = existencias.porRepuesto(repuesto.id());
            long disponible = 0;
            for (StockLote lote : lotes) {
                disponible = Math.addExact(disponible, lote.disponibleUtilizable(fecha));
            }
            if (disponible < repuesto.stockMinimo()) {
                alertas.add(new AlertaInventario("STOCK_BAJO", repuesto.id(), repuesto.nombre(),
                        null, null, null, disponible, repuesto.stockMinimo()));
            }
            lotes.stream().filter(l -> l.fisico() > 0 && l.fechaCaducidad() != null
                    && !l.fechaCaducidad().isBefore(fecha) && !l.fechaCaducidad().isAfter(limite))
                    .map(l -> new AlertaInventario("CADUCIDAD_PROXIMA", repuesto.id(), repuesto.nombre(),
                            l.loteId(), l.codigoLote(), l.fechaCaducidad(), l.fisico(), repuesto.stockMinimo()))
                    .forEach(alertas::add);
        }
        ultimas = List.copyOf(alertas);
        return ultimas;
    }

    @Override
    public void actualizar(int repuestoId) {
        consultarAlertas();
    }

    public List<AlertaInventario> ultimasAlertas() {
        return ultimas;
    }

    public String reporteConsola() {
        List<AlertaInventario> alertas = consultarAlertas();
        return alertas.isEmpty() ? "Sin alertas de inventario."
                : alertas.stream().map(AlertaInventario::toString).collect(Collectors.joining(System.lineSeparator()));
    }
}
