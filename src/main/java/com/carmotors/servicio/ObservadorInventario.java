package com.carmotors.servicio;

/** Recibe cambios de un repuesto después de confirmar la transacción. */
@FunctionalInterface
public interface ObservadorInventario {
    void actualizar(int repuestoId);

    /** Nombre utilizado por el módulo de alertas; conserva el contrato anterior. */
    default void inventarioCambio(int repuestoId) {
        actualizar(repuestoId);
    }
}
