package com.carmotors.servicio;

/** Recibe cambios de un repuesto después de confirmar la transacción. */
@FunctionalInterface
public interface ObservadorInventario {
    void actualizar(int repuestoId);
}
