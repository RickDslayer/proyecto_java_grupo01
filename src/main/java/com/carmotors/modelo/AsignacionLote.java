package com.carmotors.modelo;

import java.time.LocalDate;

/** Resultado de FEFO: cuántas unidades de un repuesto conviene tomar de un lote. */
public record AsignacionLote(int loteId, String codigo, int repuestoId, int cantidad, LocalDate fechaCaducidad) {
}