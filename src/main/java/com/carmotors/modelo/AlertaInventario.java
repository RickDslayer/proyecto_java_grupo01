package com.carmotors.modelo;

import java.time.LocalDate;

/** Resultado de solo lectura. No representa una reserva ni un consumo. */
public record AlertaInventario(String tipo, int repuestoId, String nombreRepuesto,
                              Integer loteId, String codigoLote, LocalDate caducidad,
                              long cantidad, long minimo) {
    @Override public String toString() {
        if ("STOCK_BAJO".equals(tipo)) {
            return "STOCK BAJO | " + nombreRepuesto + " (#" + repuestoId + ") | disponible: "
                    + cantidad + " | mínimo: " + minimo;
        }
        return "CADUCIDAD PROXIMA | " + nombreRepuesto + " | lote: " + codigoLote
                + " | vence: " + caducidad + " | unidades: " + cantidad;
    }
}
