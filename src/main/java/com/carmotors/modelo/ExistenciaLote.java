package com.carmotors.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que hay de UN repuesto dentro de UN lote, con su procedencia (proveedor).
 * Es un record: una clase de solo lectura cuyos getters se llaman igual que el campo
 * (por ejemplo existencia.codigo() en lugar de getCodigo()).
 * cantidadDisponible = cantidadActual menos las reservas ACTIVAS de ese lote.
 */
public record ExistenciaLote(
        int loteId, String codigo, int proveedorId, String proveedor, LocalDate fechaIngreso,
        int repuestoId, String repuesto,
        int cantidadIngresada, int cantidadActual, int cantidadDisponible,
        BigDecimal costoUnitario, LocalDate fechaCaducidad) {
}