package com.carmotors.servicio;

import java.util.ArrayList;
import java.util.List;

/** Observer: los servicios notifican sin conocer a los observadores concretos. */
public class PublicadorInventario {
    private final List<ObservadorInventario> observadores = new ArrayList<>();

    public void suscribir(ObservadorInventario observador) {
        if (observador == null) throw new IllegalArgumentException("El observador es obligatorio.");
        if (!observadores.contains(observador)) observadores.add(observador);
    }

    public void desuscribir(ObservadorInventario observador) {
        observadores.remove(observador);
    }

    /** Se llama solamente DESPUÉS del commit, nunca desde el DAO. */
    public void publicar(int repuestoId) {
        for (ObservadorInventario observador : new ArrayList<>(observadores)) {
            try {
                observador.inventarioCambio(repuestoId);
            } catch (RuntimeException error) {
                // El commit ya ocurrió: un observador no puede deshacerlo.
                System.err.println("No se pudo actualizar una alerta: " + error.getMessage());
            }
        }
    }
}
