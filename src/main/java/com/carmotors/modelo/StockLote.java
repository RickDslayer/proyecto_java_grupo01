package com.carmotors.modelo;

import java.time.LocalDate;

/** Fotografía de existencias en unidades por pareja (lote, repuesto). Solo lectura. */
public record StockLote(int loteId, String codigoLote, int repuestoId,
                        LocalDate fechaCaducidad, long fisico, long reservadoActivo) {
    public StockLote {
        if (loteId <= 0 || repuestoId <= 0 || fisico < 0 || reservadoActivo < 0 || reservadoActivo > fisico) {
            throw new IllegalArgumentException("Existencias inconsistentes para lote y repuesto");
        }
    }
    /** Diferencia contable; la caducidad no altera las unidades físicas. */
    public long disponible() { return fisico - reservadoActivo; }
    /** Caduca al final del día indicado; NULL no caduca. */
    public boolean vigente(LocalDate hoy) {
        return fechaCaducidad == null || !fechaCaducidad.isBefore(hoy);
    }
    /** Unidades que pueden proponerse para FEFO hoy. */
    public long disponibleUtilizable(LocalDate hoy) { return vigente(hoy) ? disponible() : 0; }
}
