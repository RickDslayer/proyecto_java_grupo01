package com.carmotors.servicio;

import com.carmotors.dao.AlertasDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.AlertaInventario;
import com.carmotors.modelo.RepuestoMinimo;
import com.carmotors.modelo.StockLote;
import com.carmotors.modelo.StockRepuesto;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Observador de cambios confirmados. Solo consulta: no modifica stock ni reservas. */
public class AlertasInventarioService implements ObservadorInventario {
    private final AlertasDAO dao;
    private final ExistenciasService existencias;
    private final Supplier<Connection> conexiones;
    private final Supplier<LocalDate> hoy;
    private final int diasAviso;
    private List<AlertaInventario> ultimasAlertas = new ArrayList<>();

    /** diasAviso=7 avisa de caducidades entre hoy y dentro de 7 días, inclusivos. */
    public AlertasInventarioService(AlertasDAO dao, ExistenciasService existencias,
            Supplier<Connection> conexiones, Supplier<LocalDate> hoy, int diasAviso) {
        if (dao == null || existencias == null || conexiones == null || hoy == null) {
            throw new IllegalArgumentException("Las dependencias son obligatorias.");
        }
        if (diasAviso < 0) throw new ValidacionException("Los días de aviso no pueden ser negativos.");
        this.dao = dao;
        this.existencias = existencias;
        this.conexiones = conexiones;
        this.hoy = hoy;
        this.diasAviso = diasAviso;
    }

    /** Consulta actual; no depende de haber recibido previamente algún evento. */
    public List<AlertaInventario> consultarAlertas() {
        List<RepuestoMinimo> repuestos;
        try (Connection conexion = conexiones.get()) {
            repuestos = dao.listarRepuestos(conexion);
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión de alertas.", e);
        }
        LocalDate fecha = hoy.get();
        List<AlertaInventario> alertas = new ArrayList<>();
        for (RepuestoMinimo repuesto : repuestos) {
            evaluar(repuesto, fecha, alertas);
        }
        ultimasAlertas = new ArrayList<>(alertas);
        return alertas;
    }

    private void evaluar(RepuestoMinimo repuesto, LocalDate fecha, List<AlertaInventario> alertas) {
        StockRepuesto resumen = existencias.resumen(repuesto.id());
        // Solo las unidades disponibles en lotes vigentes se pueden ofrecer al taller.
        if (resumen.disponibleUtilizable() < repuesto.stockMinimo()) {
            alertas.add(new AlertaInventario("STOCK_BAJO", repuesto.id(), repuesto.nombre(),
                    null, null, null, resumen.disponibleUtilizable(), repuesto.stockMinimo()));
        }
        LocalDate limite = fecha.plusDays(diasAviso);
        for (StockLote lote : existencias.porRepuesto(repuesto.id())) {
            LocalDate caducidad = lote.fechaCaducidad();
            if (caducidad != null && !caducidad.isBefore(fecha)
                    && !caducidad.isAfter(limite) && lote.fisico() > 0) {
                alertas.add(new AlertaInventario("CADUCIDAD_PROXIMA", repuesto.id(), repuesto.nombre(),
                        lote.loteId(), lote.codigoLote(), caducidad, lote.fisico(), 0));
            }
        }
    }

    /** Se ejecuta tras el commit; refresca la lista para reportarla desde consola. */
    @Override
    public void inventarioCambio(int repuestoId) {
        consultarAlertas();
    }

    public List<AlertaInventario> ultimasAlertas() {
        return List.copyOf(ultimasAlertas);
    }

    public String reporteConsola() {
        List<AlertaInventario> alertas = consultarAlertas();
        if (alertas.isEmpty()) return "Sin alertas de inventario.";
        StringBuilder texto = new StringBuilder("ALERTAS DE INVENTARIO\n");
        for (AlertaInventario alerta : alertas) {
            texto.append("- ").append(alerta).append("\n");
        }
        return texto.toString();
    }
}
