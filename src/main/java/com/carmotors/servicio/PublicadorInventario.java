package com.carmotors.servicio;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Los fallos de una notificación no invalidan una operación ya confirmada. */
public class PublicadorInventario {
    private final CopyOnWriteArrayList<ObservadorInventario> observadores = new CopyOnWriteArrayList<>();

    public void suscribir(ObservadorInventario observador) {
        observadores.addIfAbsent(Objects.requireNonNull(observador));
    }

    public void desuscribir(ObservadorInventario observador) {
        observadores.remove(observador);
    }

    public void publicar(int repuestoId) {
        for (ObservadorInventario observador : observadores) {
            try {
                observador.inventarioCambio(repuestoId);
            } catch (RuntimeException e) {
                System.err.println("No se pudo actualizar un observador de inventario para el repuesto #"
                        + repuestoId + ". El movimiento ya fue confirmado.");
            }
        }
    }
}
