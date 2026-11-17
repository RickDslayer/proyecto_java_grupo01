package com.carmotors.controlador;

import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.ServicioTecnico;
import com.carmotors.servicio.AsignacionTecnicoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AsignacionTecnicoController {
    private final AsignacionTecnicoService servicio;
    public AsignacionTecnicoController(AsignacionTecnicoService servicio) { this.servicio = Objects.requireNonNull(servicio); }

    public String asignar(String ordenId, String tecnicoId) {
        return ejecutar(() -> { servicio.asignar(entero(ordenId), entero(tecnicoId)); return "Técnico asignado."; });
    }
    public String quitar(String ordenId, String tecnicoId) {
        return ejecutar(() -> { servicio.quitar(entero(ordenId), entero(tecnicoId)); return "Asignación retirada."; });
    }
    public String registrarHoras(String ordenId, String tecnicoId, String horas) {
        return ejecutar(() -> {
            servicio.registrarHoras(entero(ordenId), entero(tecnicoId), new BigDecimal(horas == null ? "" : horas.trim()));
            return "Horas actualizadas.";
        });
    }
    public String listarPorOrden(String id) { return ejecutar(() -> filas(servicio.listarPorOrden(entero(id)))); }
    public String listarPorTecnico(String id) { return ejecutar(() -> filas(servicio.listarPorTecnico(entero(id)))); }

    private String filas(List<ServicioTecnico> filas) {
        return filas.isEmpty() ? "No hay asignaciones para esta consulta." : filas.stream()
                .map(f -> "Orden #" + f.servicioId() + " | técnico #" + f.tecnicoId() + " " + f.nombreTecnico()
                        + " | " + f.estadoOrden() + " | horas: " + f.horasTrabajadas().toPlainString())
                .collect(Collectors.joining(System.lineSeparator()));
    }
    private int entero(String valor) { return Integer.parseInt(valor == null ? "" : valor.trim()); }
    private String ejecutar(Supplier<String> accion) {
        try { return accion.get(); }
        catch (NumberFormatException e) { return "Error: escribe identificadores enteros y horas con punto decimal."; }
        catch (ValidacionException e) { return "Error: " + e.getMessage(); }
        catch (DaoException | ConexionException e) { return "Error del sistema: " + e.getMessage(); }
    }
}
