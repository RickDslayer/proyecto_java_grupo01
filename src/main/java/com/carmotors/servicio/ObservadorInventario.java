package com.carmotors.servicio;

/** Se entera de un cambio que ya fue confirmado en la base. */
public interface ObservadorInventario {
    void inventarioCambio(int repuestoId);
}
