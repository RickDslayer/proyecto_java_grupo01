package com.carmotors.controlador;

import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.AsignacionLote;
import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.Lote;
import com.carmotors.modelo.LoteLinea;
import com.carmotors.servicio.LoteService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Supplier;

/** Recibe texto (lo que escribe el usuario), lo convierte y llama al servicio. Devuelve mensajes listos para mostrar. */
public class LoteController {
    private final LoteService servicio;

    public LoteController(LoteService servicio) {
        this.servicio = servicio;
    }

    /** Registra un ingreso con un repuesto. fechaCaducidad vacía = sin caducidad. Fechas: AAAA-MM-DD. */
    public String registrarIngreso(String codigo, String proveedorId, String fechaIngreso,
                                   String repuestoId, String cantidad, String costoUnitario, String fechaCaducidad) {
        return ejecutar(() -> {
            LoteLinea linea = new LoteLinea();
            linea.setRepuestoId(entero(repuestoId));
            linea.setCantidadIngresada(entero(cantidad));
            linea.setCostoUnitario(decimal(costoUnitario));
            linea.setFechaCaducidad(fechaOpcional(fechaCaducidad));

            Lote lote = new Lote();
            lote.setCodigo(codigo);
            lote.setProveedorId(entero(proveedorId));
            lote.setFechaIngreso(fecha(fechaIngreso));
            lote.agregarLinea(linea);

            int id = servicio.registrarIngreso(lote);
            return "Lote registrado con id " + id + ".";
        });
    }

    public String buscarPorCodigo(String codigo) {
        return ejecutar(() -> formatear(servicio.buscarPorCodigo(codigo)));
    }

    public String listarPorRepuesto(String repuestoId) {
        return ejecutar(() -> formatear(servicio.listarPorRepuesto(entero(repuestoId))));
    }

    public String listarPorProveedor(String proveedorId) {
        return ejecutar(() -> formatear(servicio.listarPorProveedor(entero(proveedorId))));
    }

    public String consultarStock(String repuestoId) {
        return ejecutar(() -> {
            int id = entero(repuestoId);
            return "Stock físico: " + servicio.stockFisico(id) + " | Stock disponible: " + servicio.stockDisponible(id);
        });
    }

    public String seleccionarFefo(String repuestoId, String cantidad) {
        return ejecutar(() -> {
            List<AsignacionLote> plan = servicio.seleccionarFefo(entero(repuestoId), entero(cantidad));
            StringBuilder sb = new StringBuilder("Plan FEFO (solo propuesta, no reserva nada):\n");
            for (AsignacionLote a : plan) {
                sb.append("  lote ").append(a.codigo()).append(": ").append(a.cantidad()).append(" unidades (")
                        .append(a.fechaCaducidad() == null ? "sin caducidad" : "caduca " + a.fechaCaducidad())
                        .append(")\n");
            }
            return sb.toString();
        });
    }

    // ---------- Apoyo ----------

    private int entero(String texto) {
        return Integer.parseInt(texto.trim());
    }

    private BigDecimal decimal(String texto) {
        return new BigDecimal(texto.trim());
    }

    private LocalDate fecha(String texto) {
        return LocalDate.parse(texto.trim());
    }

    private LocalDate fechaOpcional(String texto) {
        return (texto == null || texto.isBlank()) ? null : fecha(texto);
    }

    private String formatear(List<ExistenciaLote> lista) {
        if (lista.isEmpty()) {
            return "No se encontraron lotes.";
        }
        StringBuilder sb = new StringBuilder();
        for (ExistenciaLote e : lista) {
            sb.append(e.codigo()).append(" | proveedor ").append(e.proveedor())
                    .append(" | ingreso ").append(e.fechaIngreso())
                    .append(" | ").append(e.repuesto())
                    .append(" | ingresadas ").append(e.cantidadIngresada())
                    .append(" | actuales ").append(e.cantidadActual())
                    .append(" | disponibles ").append(e.cantidadDisponible())
                    .append(" | ").append(e.fechaCaducidad() == null ? "sin caducidad" : "caduca " + e.fechaCaducidad())
                    .append("\n");
        }
        return sb.toString();
    }

    /** Un solo lugar donde se convierten los errores en mensajes. */
    private String ejecutar(Supplier<String> accion) {
        try {
            return accion.get();
        } catch (NumberFormatException e) {
            return "Error: escribe números válidos (ids y cantidades enteros; costos con punto, por ejemplo 25000.50).";
        } catch (DateTimeParseException e) {
            return "Error: escribe las fechas como AAAA-MM-DD, por ejemplo 2026-11-13.";
        } catch (ValidacionException e) {
            return "Error: " + e.getMessage();
        } catch (DaoException | ConexionException e) {
            return "Error del sistema: " + e.getMessage();
        }
    }
}