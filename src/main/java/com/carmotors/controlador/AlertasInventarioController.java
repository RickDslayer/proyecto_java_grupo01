package com.carmotors.controlador;

import com.carmotors.servicio.AlertasInventarioService;

/** Punto de entrada para que una futura vista de consola muestre el reporte. */
public class AlertasInventarioController {
    private final AlertasInventarioService servicio;

    public AlertasInventarioController(AlertasInventarioService servicio) {
        this.servicio = servicio;
    }

    public String mostrarReporte() {
        return servicio.reporteConsola();
    }
}
