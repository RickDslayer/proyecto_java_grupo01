package com.carmotors.controlador;

import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.RepuestoTipo;
import com.carmotors.servicio.RepuestoTipoService;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Convierte entradas de consola en llamadas al servicio y entrega mensajes. */
public final class RepuestoTipoController {
    private final RepuestoTipoService servicio;

    public RepuestoTipoController(RepuestoTipoService servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    public String crear(String nombre) {
        return ejecutar(() -> "Tipo creado con ID " + servicio.crear(nombre) + ".");
    }

    public String actualizar(String id, String nombre) {
        return ejecutar(() -> servicio.actualizar(Integer.parseInt(id.trim()), nombre)
                ? "Tipo actualizado." : "No existe el tipo indicado.");
    }

    public String eliminar(String id) {
        return ejecutar(() -> servicio.eliminar(Integer.parseInt(id.trim()))
                ? "Tipo eliminado." : "No existe el tipo indicado.");
    }

    public String listar() {
        return ejecutar(() -> formatear(servicio.listar()));
    }

    public String buscarPorId(String id) {
        return ejecutar(() -> servicio.buscarPorId(Integer.parseInt(id.trim()))
                .map(t -> t.id() + " | " + t.nombre())
                .orElse("No se encontró el tipo."));
    }

    public String buscarPorNombre(String nombre) {
        return ejecutar(() -> formatear(servicio.buscarPorNombre(nombre)));
    }

    private String formatear(List<RepuestoTipo> tipos) {
        if (tipos.isEmpty()) return "No se encontraron tipos de repuesto.";
        StringBuilder texto = new StringBuilder();
        for (RepuestoTipo tipo : tipos) texto.append(tipo.id()).append(" | ").append(tipo.nombre()).append('\n');
        return texto.toString();
    }

    private String ejecutar(Supplier<String> operacion) {
        try { return operacion.get(); }
        catch (NumberFormatException e) { return "Error: introduce un ID numérico válido."; }
        catch (ValidacionException e) { return "Error: " + e.getMessage(); }
        catch (DaoException | ConexionException e) { return "Error del sistema: " + e.getMessage(); }
    }
}
